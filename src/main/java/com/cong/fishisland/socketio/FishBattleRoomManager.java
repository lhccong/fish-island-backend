package com.cong.fishisland.socketio;

import cn.hutool.core.util.RandomUtil;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 摸鱼大乱斗内存房间管理器。
 * 管理 Socket.IO 连接与房间的实时映射关系。
 * 所有房间/大厅数据均在内存中维护，游戏开始前不涉及任何DB操作。
 */
@Slf4j
@Component
public class FishBattleRoomManager {

    /**
     * roomCode -> RoomSession 的映射
     */
    private final Map<String, RoomSession> rooms = new ConcurrentHashMap<>();

    /**
     * sessionId -> PlayerConnection 的映射（快速反查）
     */
    private final Map<String, PlayerConnection> sessionMap = new ConcurrentHashMap<>();

    /**
     * userId -> roomCode 的映射（防止同一用户重复加入多个房间）
     */
    private final Map<Long, String> userRoomIndex = new ConcurrentHashMap<>();

    /**
     * 所有已认证连接（sessionId -> userId）—— 独立于 lobby/room 状态，
     * 仅在 onConnect/onDisconnect 时更新，确保 getOnlineCount 在任何过渡态都准确。
     */
    private final Map<String, Long> connectedUsers = new ConcurrentHashMap<>();

    /**
     * 所有已认证连接的客户端（sessionId -> SocketIOClient）——
     * 用于全局广播（大厅概览、房间变更等），确保任何状态下的玩家都能收到事件。
     */
    private final Map<String, SocketIOClient> connectedClients = new ConcurrentHashMap<>();

    /**
     * 房间级锁对象映射（roomCode -> lock），用于保护同一房间的并发操作
     */
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();

    /**
     * 已结束房间定时清理调度器
     */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "fish-battle-room-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 已结束房间保留时长（毫秒），超过后从内存移除
     */
    private static final long ENDED_ROOM_TTL_MS = 5 * 60 * 1000L; // 5分钟

    /**
     * 对局中全员离线后的宽限期（秒），在此期间如有玩家重连则取消销毁
     */
    private static final int ALL_OFFLINE_GRACE_SECONDS = 30;

    /**
     * 正在等待全员离线销毁的房间集合（roomCode -> 标记），用于在玩家重连时取消延迟销毁
     */
    private final Map<String, Boolean> pendingAllOfflineDestroy = new ConcurrentHashMap<>();

    {
        // 每60秒清理一次已结束超时的房间
        cleanupScheduler.scheduleAtFixedRate(this::cleanupEndedRooms, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 房间延迟销毁回调接口（用于通知 SocketHandler 执行广播）
     */
    public interface RoomDestroyedCallback {
        void onRoomDestroyed(String roomCode, Long endedDbRoomId);
    }

    private volatile RoomDestroyedCallback roomDestroyedCallback;

    public void setRoomDestroyedCallback(RoomDestroyedCallback callback) {
        this.roomDestroyedCallback = callback;
    }

    /**
     * 延迟执行全员离线房间销毁（由 scheduler 线程调用）
     */
    private void executeAllOfflineDestroy(String roomCode) {
        // 无论是否销毁，都从 pending 中移除
        pendingAllOfflineDestroy.remove(roomCode);

        RoomSession roomSession = rooms.get(roomCode);
        if (roomSession == null) {
            return;
        }

        synchronized (getRoomLock(roomCode)) {
            // 再次检查：如果有玩家已重连，取消销毁
            boolean stillAllOffline = roomSession.getPlayers().values().stream()
                    .noneMatch(PlayerConnection::isOnline);
            if (!stillAllOffline) {
                log.info("全员离线宽限期结束，已有玩家重连，取消销毁: roomCode={}", roomCode);
                return;
            }
            // 确认全员仍离线，执行销毁
            log.info("全员离线宽限期结束，确认销毁房间: roomCode={}", roomCode);
            roomSession.setStatus(3);
            roomSession.setEndedTime(System.currentTimeMillis());
            Long endedDbRoomId = roomSession.getDbRoomId();
            for (PlayerConnection p : roomSession.getPlayers().values()) {
                userRoomIndex.remove(p.getUserId());
            }
            // 通过回调通知 SocketHandler 执行广播
            if (roomDestroyedCallback != null) {
                roomDestroyedCallback.onRoomDestroyed(roomCode, endedDbRoomId);
            }
        }
    }

    /* ==================== 房间创建与查询 ==================== */

    /**
     * 纯内存创建房间
     */
    public RoomSession createRoom(String roomName, String gameMode, boolean aiFillEnabled,
                                  Long creatorId, String creatorName) {
        String roomCode = generateRoomCode();
        RoomSession session = new RoomSession();
        session.setRoomCode(roomCode);
        session.setRoomName(roomName);
        session.setGameMode(gameMode);
        session.setMaxPlayers(10);
        session.setAiFillEnabled(aiFillEnabled);
        session.setCreatorId(creatorId);
        session.setCreatorName(creatorName);
        session.setStatus(0);
        session.setCreateTime(System.currentTimeMillis());
        rooms.put(roomCode, session);
        log.info("房间已创建(内存): roomCode={}, roomName={}, creator={}", roomCode, roomName, creatorName);
        return session;
    }

    /**
     * 获取所有活跃房间的快照列表（大厅用）
     */
    public List<Map<String, Object>> getAllActiveRoomsSnapshot() {
        return rooms.values().stream()
                .filter(s -> s.getStatus() <= 2)
                .sorted((a, b) -> {
                    // 等待中排前面，再按创建时间降序
                    if (a.getStatus() != b.getStatus()) {
                        return Integer.compare(a.getStatus(), b.getStatus());
                    }
                    return Long.compare(b.getCreateTime(), a.getCreateTime());
                })
                .map(this::buildRoomSnapshot)
                .collect(Collectors.toList());
    }

    /**
     * 获取内存中等待中(status=0)房间的快照列表
     */
    public List<Map<String, Object>> getWaitingRoomsSnapshot() {
        return rooms.values().stream()
                .filter(s -> s.getStatus() == 0)
                .sorted((a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()))
                .map(this::buildRoomSnapshot)
                .collect(Collectors.toList());
    }

    /**
     * 获取单个房间信息快照（房间详情用）
     */
    public Map<String, Object> getRoomInfoSnapshot(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return null;
        }
        return buildRoomSnapshot(session);
    }

    /**
     * 获取活跃房间总数
     */
    public int getActiveRoomCount() {
        return (int) rooms.values().stream()
                .filter(s -> s.getStatus() <= 2)
                .count();
    }

    /**
     * 检查房间是否存在
     */
    public boolean roomExists(String roomCode) {
        return rooms.containsKey(roomCode);
    }

    /**
     * 获取房间会话
     */
    public RoomSession getRoomSession(String roomCode) {
        return rooms.get(roomCode);
    }

    /**
     * 构建房间快照Map
     */
    private Map<String, Object> buildRoomSnapshot(RoomSession session) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("roomCode", session.getRoomCode());
        item.put("roomName", session.getRoomName());
        item.put("status", session.getStatus());
        item.put("gameMode", session.getGameMode());
        item.put("maxPlayers", session.getMaxPlayers());
        item.put("currentPlayers", session.getPlayers().size());
        item.put("aiFillEnabled", session.isAiFillEnabled() ? 1 : 0);
        item.put("creatorId", session.getCreatorId());
        item.put("creatorName", session.getCreatorName());
        item.put("createTime", session.getCreateTime());
        item.put("isHeroPickPhase", session.isHeroPickPhase());
        item.put("isLoadingPhase", session.isLoadingPhase());
        return item;
    }

    /**
     * 生成6位随机房间编码（确保唯一性）
     */
    private String generateRoomCode() {
        String code;
        do {
            code = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ23456789", 6);
        } while (rooms.containsKey(code));
        return code;
    }

    /* ==================== userId 索引管理 ==================== */

    /**
     * 查找用户当前所在房间（如果有的话）
     * @return roomCode，null 表示不在任何房间中
     */
    public String findRoomByUserId(Long userId) {
        String roomCode = userRoomIndex.get(userId);
        if (roomCode != null) {
            // 验证房间和玩家确实存在（防止索引脏数据）
            RoomSession session = rooms.get(roomCode);
            if (session != null) {
                boolean found = session.getPlayers().values().stream()
                        .anyMatch(p -> userId.equals(p.getUserId()));
                if (found) {
                    return roomCode;
                }
            }
            // 索引脏数据，清理
            userRoomIndex.remove(userId);
        }
        return null;
    }

    /**
     * 获取房间级锁对象
     */
    private Object getRoomLock(String roomCode) {
        return roomLocks.computeIfAbsent(roomCode, k -> new Object());
    }

    /* ==================== 玩家加入/离开 ==================== */

    /**
     * 玩家加入房间（带队伍和位置）— 带 synchronized 防并发 + userId 去重
     * @return PlayerConnection，null 表示加入失败
     */
    public PlayerConnection joinRoom(String roomCode, Long userId, String playerName,
                                     String userAvatar, String team, int slotIndex, SocketIOClient client) {
        String sessionId = client.getSessionId().toString();

        RoomSession roomSession = rooms.get(roomCode);
        if (roomSession == null) {
            return null;
        }

        synchronized (getRoomLock(roomCode)) {
            // 二次检查房间状态（可能在等待锁期间已变化）
            if (roomSession.getStatus() != 0) {
                return null;
            }

            // 检查同一用户是否已在此房间中（多标签页场景）
            for (PlayerConnection existing : roomSession.getPlayers().values()) {
                if (userId.equals(existing.getUserId())) {
                    // 已在此房间：更新连接信息（替换旧session）
                    String oldSessionId = existing.getSessionId();
                    sessionMap.remove(oldSessionId);
                    existing.setSessionId(sessionId);
                    existing.setClient(client);
                    existing.setOnline(true);
                    existing.setPlayerName(playerName);
                    existing.setUserAvatar(userAvatar);
                    roomSession.getPlayers().remove(oldSessionId);
                    roomSession.getPlayers().put(sessionId, existing);
                    sessionMap.put(sessionId, existing);
                    log.info("玩家重复加入房间(替换旧连接): roomCode={}, userId={}, oldSession={}, newSession={}",
                            roomCode, userId, oldSessionId, sessionId);
                    return existing;
                }
            }

            // 检查位置是否已被占用（synchronized 内保证原子性）
            boolean slotTaken = roomSession.getPlayers().values().stream()
                    .anyMatch(p -> team.equals(p.getTeam()) && p.getSlotIndex() == slotIndex);
            if (slotTaken) {
                return null;
            }

            // 检查队伍人数上限
            long teamCount = roomSession.getPlayers().values().stream()
                    .filter(p -> team.equals(p.getTeam())).count();
            if (teamCount >= 5) {
                return null;
            }

            PlayerConnection conn = new PlayerConnection();
            conn.setSessionId(sessionId);
            conn.setUserId(userId);
            conn.setPlayerName(playerName);
            conn.setUserAvatar(userAvatar);
            conn.setRoomCode(roomCode);
            conn.setTeam(team);
            conn.setSlotIndex(slotIndex);
            conn.setReady(false);
            conn.setClient(client);

            roomSession.getPlayers().put(sessionId, conn);
            sessionMap.put(sessionId, conn);
            userRoomIndex.put(userId, roomCode);

            log.info("玩家加入房间: roomCode={}, userId={}, team={}, slot={}", roomCode, userId, team, slotIndex);
            return conn;
        }
    }

    /**
     * 检查位置是否已被占用
     */
    public boolean isSlotOccupied(String roomCode, String team, int slotIndex) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return false;
        }
        return session.getPlayers().values().stream()
                .anyMatch(p -> team.equals(p.getTeam()) && p.getSlotIndex() == slotIndex);
    }

    /**
     * 获取队伍人数
     */
    public long getTeamCount(String roomCode, String team) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return 0;
        }
        return session.getPlayers().values().stream()
                .filter(p -> team.equals(p.getTeam())).count();
    }

    /**
     * 切换位置（纯内存，带 synchronized 防并发）
     * @return true 切换成功，false 位置已被占用或队伍已满
     */
    public boolean switchSlot(String sessionId, String newTeam, int newSlotIndex) {
        PlayerConnection conn = sessionMap.get(sessionId);
        if (conn == null) {
            return false;
        }
        String roomCode = conn.getRoomCode();

        synchronized (getRoomLock(roomCode)) {
            // 同位置无需切换
            if (newTeam.equals(conn.getTeam()) && newSlotIndex == conn.getSlotIndex()) {
                return true;
            }
            // 检查目标位置是否已被占用
            if (isSlotOccupied(roomCode, newTeam, newSlotIndex)) {
                return false;
            }
            // 跨队切换检查队伍人数上限
            if (!newTeam.equals(conn.getTeam()) && getTeamCount(roomCode, newTeam) >= 5) {
                return false;
            }
            conn.setTeam(newTeam);
            conn.setSlotIndex(newSlotIndex);
            conn.setReady(false);
            return true;
        }
    }

    /**
     * 设置准备状态（纯内存）
     */
    public boolean setReady(String sessionId, boolean ready) {
        PlayerConnection conn = sessionMap.get(sessionId);
        if (conn == null) {
            return false;
        }
        conn.setReady(ready);
        return true;
    }

    /**
     * 获取房间内玩家快照（用于广播给前端）
     */
    public List<Map<String, Object>> getRoomPlayersSnapshot(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.getPlayers().values().stream().map(p -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", p.getUserId());
            item.put("playerName", p.getPlayerName());
            item.put("userAvatar", p.getUserAvatar());
            item.put("team", p.getTeam());
            item.put("slotIndex", p.getSlotIndex());
            item.put("isReady", p.isReady());
            item.put("isOnline", p.isOnline());
            item.put("isAi", false);
            item.put("selectedHeroId", p.getSelectedHeroId());
            item.put("heroConfirmed", p.isHeroConfirmed());
            item.put("heroName", p.getHeroName());
            item.put("heroAvatarUrl", p.getHeroAvatarUrl());
            item.put("heroSplashArt", p.getHeroSplashArt());
            item.put("heroRole", p.getHeroRole());
            item.put("skinId", p.getSkinId());
            item.put("skinSplashArt", p.getSkinSplashArt());
            item.put("skinModelUrl", p.getSkinModelUrl());
            item.put("spell1", p.getSpell1() != null ? p.getSpell1() : "flash");
            item.put("spell2", p.getSpell2() != null ? p.getSpell2() : "heal");
            item.put("loadingProgress", p.getLoadingProgress());
            item.put("loaded", p.isLoaded());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 获取房间内玩家数
     */
    public int getRoomPlayerCount(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        return session == null ? 0 : session.getPlayers().size();
    }

    /**
     * 玩家离开房间（纯内存，包含房主转移逻辑）
     * 对于已在对局中（status>=1）的房间，不真正移除玩家，仅标记离线，支持后续重连。
     * @return LeaveResult 包含离开者信息和可能的新房主信息
     */
    public LeaveResult leaveRoom(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        PlayerConnection conn = sessionMap.get(sessionId);
        if (conn == null) {
            return null;
        }
        String roomCode = conn.getRoomCode();
        RoomSession roomSession = rooms.get(roomCode);
        LeaveResult result = new LeaveResult();
        result.setLeavingPlayer(conn);

        if (roomSession != null) {
            synchronized (getRoomLock(roomCode)) {
                // 对局中（status>=1）的房间：不移除玩家，仅标记离线
                if (roomSession.getStatus() >= 1) {
                    conn.setOnline(false);
                    conn.setClient(null);
                    // 从 sessionMap 中移除旧 sessionId 映射（重连时会建立新的映射）
                    sessionMap.remove(sessionId);
                    result.setDisconnectedInGame(true);
                    log.info("玩家断连(对局中保留): roomCode={}, userId={}, sessionId={}", roomCode, conn.getUserId(), sessionId);
                    // 对局中不转移房主、不移除房间、不清理 userRoomIndex（需要用于重连查找）

                    // 检测是否所有玩家都已离线：如果是，延迟销毁（给予重连宽限期）
                    boolean allOffline = roomSession.getPlayers().values().stream()
                            .noneMatch(PlayerConnection::isOnline);
                    if (allOffline && !pendingAllOfflineDestroy.containsKey(roomCode)) {
                        log.info("对局中房间全员离线，启动{}秒宽限期: roomCode={}", ALL_OFFLINE_GRACE_SECONDS, roomCode);
                        pendingAllOfflineDestroy.put(roomCode, true);
                        final String rc = roomCode;
                        cleanupScheduler.schedule(() -> executeAllOfflineDestroy(rc),
                                ALL_OFFLINE_GRACE_SECONDS, TimeUnit.SECONDS);
                    }
                } else {
                    // 等待中（status=0）的房间：真正移除玩家
                    sessionMap.remove(sessionId);
                    roomSession.getPlayers().remove(sessionId);
                    userRoomIndex.remove(conn.getUserId());

                    if (roomSession.getPlayers().isEmpty()) {
                        // 房间空了，移除房间
                        rooms.remove(roomCode);
                        roomLocks.remove(roomCode);
                        result.setRoomRemoved(true);
                        log.info("房间已移除(无人): roomCode={}", roomCode);
                    } else {
                        // 检查是否需要转移房主
                        if (conn.getUserId().equals(roomSession.getCreatorId())) {
                            // 找到最早加入的其他玩家作为新房主
                            PlayerConnection newOwner = roomSession.getPlayers().values().iterator().next();
                            if (newOwner != null) {
                                roomSession.setCreatorId(newOwner.getUserId());
                                roomSession.setCreatorName(newOwner.getPlayerName());
                                result.setNewOwnerId(newOwner.getUserId());
                                result.setNewOwnerName(newOwner.getPlayerName());
                                log.info("房主转移(内存): roomCode={}, newOwner={}", roomCode, newOwner.getUserId());
                            }
                        }
                    }
                }
            }
        } else {
            // 房间不存在，清理残留索引
            sessionMap.remove(sessionId);
            userRoomIndex.remove(conn.getUserId());
        }

        log.info("玩家离开房间: roomCode={}, userId={}, sessionId={}", roomCode, conn.getUserId(), sessionId);
        return result;
    }

    /**
     * 玩家重连房间（对局中断线重连）
     * 根据 userId 查找该玩家在内存房间中的记录，更新其 sessionId、client、online 状态。
     * @return 重连后的 PlayerConnection，null 表示该用户不在此房间中
     */
    public PlayerConnection rejoinRoom(String roomCode, Long userId, SocketIOClient client) {
        RoomSession roomSession = rooms.get(roomCode);
        if (roomSession == null) {
            return null;
        }
        // 在房间的玩家列表中查找该 userId
        PlayerConnection existingConn = null;
        String oldKey = null;
        for (Map.Entry<String, PlayerConnection> entry : roomSession.getPlayers().entrySet()) {
            if (entry.getValue().getUserId().equals(userId)) {
                existingConn = entry.getValue();
                oldKey = entry.getKey();
                break;
            }
        }
        if (existingConn == null) {
            return null;
        }

        String newSessionId = client.getSessionId().toString();

        // 从旧 key 移除，用新 sessionId 作为 key 重新放入
        if (oldKey != null && !oldKey.equals(newSessionId)) {
            roomSession.getPlayers().remove(oldKey);
            sessionMap.remove(oldKey);
        }

        // 更新连接信息
        existingConn.setSessionId(newSessionId);
        existingConn.setClient(client);
        existingConn.setOnline(true);

        // 重新注册映射
        roomSession.getPlayers().put(newSessionId, existingConn);
        sessionMap.put(newSessionId, existingConn);

        // 取消全员离线延迟销毁（有玩家重连了）
        if (pendingAllOfflineDestroy.remove(roomCode) != null) {
            log.info("玩家重连，取消全员离线延迟销毁: roomCode={}, userId={}", roomCode, userId);
        }

        log.info("玩家重连房间: roomCode={}, userId={}, newSessionId={}", roomCode, userId, newSessionId);
        return existingConn;
    }

    /**
     * 检查指定用户是否在内存房间中（根据 userId 查找）
     */
    public boolean isUserInRoom(String roomCode, Long userId) {
        RoomSession roomSession = rooms.get(roomCode);
        if (roomSession == null) {
            return false;
        }
        return roomSession.getPlayers().values().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
    }

    /**
     * 根据 sessionId 获取玩家连接信息
     */
    public PlayerConnection getBySessionId(String sessionId) {
        return sessionMap.get(sessionId);
    }

    /**
     * 获取房间内所有在线客户端
     */
    public Collection<SocketIOClient> getRoomClients(String roomCode) {
        RoomSession roomSession = rooms.get(roomCode);
        if (roomSession == null) {
            return java.util.Collections.emptyList();
        }
        return roomSession.getPlayers().values().stream()
                .map(PlayerConnection::getClient)
                .filter(c -> c != null && c.isChannelOpen())
                .collect(Collectors.toList());
    }

    /**
     * 获取房间内所有玩家连接
     */
    public Collection<PlayerConnection> getRoomPlayers(String roomCode) {
        RoomSession roomSession = rooms.get(roomCode);
        if (roomSession == null) {
            return java.util.Collections.emptyList();
        }
        return roomSession.getPlayers().values();
    }

    /**
     * 获取当前在线总人数（按 userId 去重）。
     * 基于 connectedUsers（所有已认证的 Socket 连接）统计，
     * 不依赖 sessionMap，避免房间↔大厅过渡期的计数盲区。
     */
    public int getOnlineCount() {
        return (int) connectedUsers.values().stream().distinct().count();
    }

    /**
     * 注册已认证连接（在 onConnect 鉴权成功后调用）
     */
    public void addConnectedUser(String sessionId, Long userId, SocketIOClient client) {
        connectedUsers.put(sessionId, userId);
        connectedClients.put(sessionId, client);
    }

    /**
     * 移除已断开的连接（在 onDisconnect 时调用）
     */
    public void removeConnectedUser(String sessionId) {
        connectedUsers.remove(sessionId);
        connectedClients.remove(sessionId);
    }

    /**
     * 获取所有已认证且连接有效的客户端（用于全局广播）
     */
    public Collection<SocketIOClient> getAllConnectedClients() {
        return connectedClients.values().stream()
                .filter(c -> c != null && c.isChannelOpen())
                .collect(Collectors.toList());
    }

    /**
     * 获取当前战斗中玩家数（仅统计在线玩家，已断线的不计入）
     */
    public int getFightingPlayerCount() {
        return (int) rooms.values().stream()
                .filter(s -> s.getStatus() == 1 || s.getStatus() == 2)
                .flatMap(s -> s.getPlayers().values().stream())
                .filter(PlayerConnection::isOnline)
                .count();
    }

    /**
     * 获取当前战斗中玩家列表快照
     */
    public List<Map<String, Object>> getFightingPlayersSnapshot() {
        // 按 userId 去重，仅包含在线玩家
        Map<Long, String> uniquePlayers = new LinkedHashMap<>();
        rooms.values().stream()
                .filter(s -> s.getStatus() == 1 || s.getStatus() == 2)
                .flatMap(s -> s.getPlayers().values().stream())
                .filter(PlayerConnection::isOnline)
                .forEach(p -> uniquePlayers.putIfAbsent(p.getUserId(), p.getPlayerName()));
        return uniquePlayers.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", e.getKey());
                    item.put("userName", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查房间是否满足开始游戏条件：两队各至少1人，且所有人已准备
     */
    public boolean canStartGame(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getPlayers().isEmpty()) {
            return false;
        }
        long blueCount = 0;
        long redCount = 0;
        for (PlayerConnection p : session.getPlayers().values()) {
            if (!p.isReady()) {
                return false;
            }
            if ("blue".equals(p.getTeam())) {
                blueCount++;
            } else if ("red".equals(p.getTeam())) {
                redCount++;
            }
        }
        return blueCount >= 1 && redCount >= 1;
    }

    /**
     * 设置房间状态
     */
    public void setRoomStatus(String roomCode, int status) {
        RoomSession session = rooms.get(roomCode);
        if (session != null) {
            session.setStatus(status);
        }
    }

    /* ==================== 选英雄阶段 ==================== */

    /**
     * 开始选英雄阶段
     */
    public void startHeroPick(String roomCode, int durationSeconds) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return;
        }
        session.setStatus(1); // 1=选英雄中
        session.setHeroPickPhase(true);
        session.setLoadingPhase(false);
        session.setHeroPickDeadline(System.currentTimeMillis() + durationSeconds * 1000L);
        // 重置所有玩家的英雄选择状态
        for (PlayerConnection p : session.getPlayers().values()) {
            p.setSelectedHeroId(null);
            p.setHeroConfirmed(false);
            p.setHeroName(null);
            p.setHeroAvatarUrl(null);
            p.setHeroSplashArt(null);
            p.setHeroRole(null);
            p.setSkinId(null);
            p.setSkinSplashArt(null);
            p.setSkinModelUrl(null);
            p.setLoadingProgress(0);
            p.setLoaded(false);
        }
        log.info("选英雄阶段开始: roomCode={}, deadline={}s", roomCode, durationSeconds);
    }

    /**
     * 玩家选择英雄（纯内存，未确认可以更换）
     */
    public boolean selectHero(String sessionId, String heroId, String heroName,
                              String heroAvatarUrl, String heroSplashArt, String heroRole,
                              String heroModelUrl) {
        PlayerConnection conn = sessionMap.get(sessionId);
        if (conn == null) {
            return false;
        }
        // 已确认锁定的不能再换
        if (conn.isHeroConfirmed()) {
            return false;
        }
        conn.setSelectedHeroId(heroId);
        conn.setHeroName(heroName);
        conn.setHeroAvatarUrl(heroAvatarUrl);
        conn.setHeroSplashArt(heroSplashArt);
        conn.setHeroRole(heroRole);
        conn.setHeroModelUrl(heroModelUrl);
        return true;
    }

    /**
     * 玩家确认锁定英雄
     */
    public boolean confirmHero(String sessionId) {
        PlayerConnection conn = sessionMap.get(sessionId);
        if (conn == null || conn.getSelectedHeroId() == null) {
            return false;
        }
        conn.setHeroConfirmed(true);
        return true;
    }

    /**
     * 玩家取消锁定英雄
     */
    public boolean unconfirmHero(String sessionId) {
        PlayerConnection conn = sessionMap.get(sessionId);
        if (conn == null || !conn.isHeroConfirmed()) {
            return false;
        }
        conn.setHeroConfirmed(false);
        return true;
    }

    /**
     * 检查房间是否所有玩家都已确认英雄
     */
    public boolean isAllHeroesConfirmed(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getPlayers().isEmpty()) {
            return false;
        }
        return session.getPlayers().values().stream().allMatch(PlayerConnection::isHeroConfirmed);
    }

    /**
     * 获取选英雄阶段快照（用于广播给前端）
     */
    public List<Map<String, Object>> getHeroPickSnapshot(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.getPlayers().values().stream().map(p -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", p.getUserId());
            item.put("playerName", p.getPlayerName());
            item.put("userAvatar", p.getUserAvatar());
            item.put("team", p.getTeam());
            item.put("slotIndex", p.getSlotIndex());
            item.put("selectedHeroId", p.getSelectedHeroId());
            item.put("heroConfirmed", p.isHeroConfirmed());
            item.put("heroName", p.getHeroName());
            item.put("heroAvatarUrl", p.getHeroAvatarUrl());
            item.put("heroSplashArt", p.getHeroSplashArt());
            item.put("heroRole", p.getHeroRole());
            item.put("skinId", p.getSkinId());
            item.put("skinSplashArt", p.getSkinSplashArt());
            item.put("skinModelUrl", p.getSkinModelUrl());
            item.put("spell1", p.getSpell1() != null ? p.getSpell1() : "flash");
            item.put("spell2", p.getSpell2() != null ? p.getSpell2() : "heal");
            item.put("loadingProgress", p.getLoadingProgress());
            item.put("loaded", p.isLoaded());
            return item;
        }).collect(Collectors.toList());
    }

    public boolean isLoadingPhase(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        return session != null && session.isLoadingPhase();
    }

    public void startLoadingPhase(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return;
        }
        session.setHeroPickPhase(false);
        session.setLoadingPhase(true);
        for (PlayerConnection player : session.getPlayers().values()) {
            player.setLoadingProgress(0);
            player.setLoaded(false);
        }
    }

    public boolean updateLoadingProgress(String roomCode, Long userId, int progress) {
        RoomSession session = rooms.get(roomCode);
        if (session == null || !session.isLoadingPhase()) {
            return false;
        }
        PlayerConnection player = session.getPlayers().values().stream()
                .filter(p1 -> p1.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (player == null) {
            return false;
        }
        int nextProgress = Math.max(player.getLoadingProgress(), Math.max(0, Math.min(progress, 100)));
        player.setLoadingProgress(nextProgress);
        return true;
    }

    public boolean markPlayerLoaded(String roomCode, Long userId) {
        RoomSession session = rooms.get(roomCode);
        if (session == null || !session.isLoadingPhase()) {
            return false;
        }
        PlayerConnection player = session.getPlayers().values().stream()
                .filter(p1 -> p1.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (player == null) {
            return false;
        }
        player.setLoadingProgress(100);
        player.setLoaded(true);
        return true;
    }

    public boolean areAllPlayersLoaded(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null || session.getPlayers().isEmpty() || !session.isLoadingPhase()) {
            return false;
        }
        return session.getPlayers().values().stream().allMatch(PlayerConnection::isLoaded);
    }

    public int getLoadedPlayerCount(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return 0;
        }
        return (int) session.getPlayers().values().stream().filter(PlayerConnection::isLoaded).count();
    }

    /**
     * 选择皮肤
     */
    public boolean selectSkin(String roomCode, Long userId, String skinId, String skinSplashArt, String skinModelUrl) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) return false;
        PlayerConnection player = session.getPlayers().values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (player == null || player.isHeroConfirmed()) return false;
        player.setSkinId(skinId);
        player.setSkinSplashArt(skinSplashArt);
        player.setSkinModelUrl(skinModelUrl);
        return true;
    }

    /**
     * 选择召唤师技能
     */
    public boolean selectSpells(String roomCode, Long userId, String spell1, String spell2) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) return false;
        PlayerConnection player = session.getPlayers().values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (player == null) return false;
        player.setSpell1(spell1);
        player.setSpell2(spell2);
        return true;
    }

    /**
     * 结束选英雄阶段（不再直接改为对局中，等加载完成后再改）
     */
    public void finishHeroPick(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return;
        }
        session.setHeroPickPhase(false);
        session.setLoadingPhase(true);
        for (PlayerConnection player : session.getPlayers().values()) {
            player.setLoadingProgress(0);
            player.setLoaded(false);
        }
        // 注意：不再在此处设status=2，等所有玩家加载完成后再设
        log.info("选英雄阶段结束: roomCode={}", roomCode);
    }

    /**
     * 设置房间进入对局状态（所有玩家加载完成后调用）
     */
    public void enterPlaying(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) return;
        session.setLoadingPhase(false);
        session.setStatus(2); // 2=对局中
        log.info("进入对局: roomCode={}", roomCode);
    }

    /* ==================== 房间结束与清理 ==================== */

    /**
     * 标记房间为已结束（status=3），并清理玩家索引。
     * 房间本身暂时保留在内存中供断线重连检查，由定时任务稍后清除。
     */
    public void endRoom(String roomCode) {
        RoomSession session = rooms.get(roomCode);
        if (session == null) {
            return;
        }
        synchronized (getRoomLock(roomCode)) {
            session.setStatus(3);
            session.setEndedTime(System.currentTimeMillis());
            // 清理所有玩家的 sessionMap 和 userRoomIndex
            for (PlayerConnection p : session.getPlayers().values()) {
                if (p.getSessionId() != null) {
                    sessionMap.remove(p.getSessionId());
                }
                userRoomIndex.remove(p.getUserId());
            }
        }
        log.info("房间已标记为结束: roomCode={}", roomCode);
    }

    /**
     * 定时清理已结束且超过保留时长的房间
     */
    private void cleanupEndedRooms() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, RoomSession> entry : rooms.entrySet()) {
            RoomSession session = entry.getValue();
            if (session.getStatus() == 3 && session.getEndedTime() > 0
                    && (now - session.getEndedTime()) > ENDED_ROOM_TTL_MS) {
                toRemove.add(entry.getKey());
            }
        }
        for (String code : toRemove) {
            rooms.remove(code);
            roomLocks.remove(code);
            log.debug("已清理过期结束房间: roomCode={}", code);
        }
        if (!toRemove.isEmpty()) {
            log.info("定时清理：移除 {} 个已结束房间", toRemove.size());
        }
    }

    /* ==================== 内部数据类 ==================== */

    /**
     * 房间会话（包含房间元数据 + 玩家列表）
     */
    @Data
    public static class RoomSession {
        private String roomCode;
        private String roomName;
        private String gameMode;
        private int maxPlayers;
        private boolean aiFillEnabled;
        private Long creatorId;
        private String creatorName;
        private int status; // 0等待中 1选英雄中 2对局中 3已结束
        private long createTime;
        private long endedTime; // 房间结束时间戳（status=3时设置，用于定时清理）
        private Long dbRoomId; // 落库后的DB房间ID，游戏开始后赋值
        private boolean heroPickPhase; // 是否处于选英雄阶段
        private boolean loadingPhase; // 是否处于加载阶段
        private long heroPickDeadline; // 选英雄倒计时截止时间戳
        private final Map<String, PlayerConnection> players = new ConcurrentHashMap<>();
    }

    /**
     * 玩家连接信息
     */
    @Data
    public static class PlayerConnection {
        private String sessionId;
        private Long userId;
        private String playerName;
        private String userAvatar;
        private String roomCode;
        private String team;
        private int slotIndex;
        private boolean ready;
        private boolean online = true; // 是否在线（断连后标记为false，重连后恢复为true）
        private SocketIOClient client;
        // 选英雄阶段字段
        private String selectedHeroId;
        private boolean heroConfirmed;
        private String heroName;
        private String heroAvatarUrl;
        private String heroSplashArt;
        private String heroRole;
        private String heroModelUrl;
        // 皮肤和召唤师技能
        private String skinId;
        private String skinSplashArt;
        private String skinModelUrl;
        private String spell1;
        private String spell2;
        private int loadingProgress;
        private boolean loaded;
    }

    /**
     * 离开房间结果
     */
    @Data
    public static class LeaveResult {
        private PlayerConnection leavingPlayer;
        private boolean roomRemoved;
        private boolean disconnectedInGame; // 对局中断连（非真正离开）
        private Long newOwnerId;
        private String newOwnerName;
        private Long endedDbRoomId; // 全员离线自动结束时的DB房间ID，用于同步更新DB状态
    }
}
