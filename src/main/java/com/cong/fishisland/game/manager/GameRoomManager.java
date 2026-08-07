package com.cong.fishisland.game.manager;

import com.cong.fishisland.game.cache.GameRoomRedisCache;
import com.cong.fishisland.game.cache.GameSessionRedisCache;
import com.cong.fishisland.game.enums.GameMessageTypeEnum;
import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import com.cong.fishisland.game.model.GameSession;
import com.cong.fishisland.game.model.player.GamePlayer;
import com.cong.fishisland.game.model.room.GameRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏房间管理器
 * 数据存储改用 Redis，支持服务重启恢复
 *
 * @author cong
 */
@Slf4j
@Component
public class GameRoomManager {

    @Resource
    @Lazy
    private GameSessionManager sessionManager;

    @Resource
    private GameRoomRedisCache roomCache;

    @Resource
    private GameSessionRedisCache sessionCache;

    /**
     * 房间内存缓存（用于减少 Redis 访问，提升性能）
     */
    private final Map<String, GameRoom> roomMemoryCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 重连窗口时间（毫秒）：玩家断线后保留会话的时间
     */
    private static final long RECONNECT_WINDOW_MS = 60000L;

    /**
     * 玩家离线超时（毫秒）：超过这个时间自动移除
     */
    private static final long OFFLINE_TIMEOUT_MS = 120000L;

    @PostConstruct
    public void init() {
        try {
            int roomCount = 0;
            int userRoomCount = 0;
            int sessionCount = 0;

            // 恢复房间数据
            List<GameRoom> rooms = roomCache.getAllRooms();
            for (GameRoom room : rooms) {
                roomMemoryCache.put(room.getRoomId(), room);
                roomCount++;
            }

            // 恢复会话数据（用户会话主要用于断线重连）
            List<GameSession> sessions = sessionCache.getAllSessions();
            for (GameSession session : sessions) {
                sessionCount++;
            }

            // 验证 user-room 映射
            Map<Long, String> userRooms = roomCache.getAllUserRooms();
            userRoomCount = userRooms.size();

                    roomCount, userRoomCount, sessionCount);
        } catch (Exception e) {
            log.error("从 Redis 恢复游戏数据失败", e);
        }
    }

    // ==================== 房间管理 ====================

    /**
     * 创建房间
     */
    public GameRoom createRoom(GameTypeEnum gameType, Long ownerId, String ownerName, String ownerAvatar) {
        String roomId = generateRoomId();

        GameRoom room = new GameRoom(roomId, gameType, ownerId);

        // 添加房主
        GamePlayer owner = new GamePlayer(ownerId, ownerName, ownerAvatar);
        owner.setRole(com.cong.fishisland.game.enums.PlayerRoleEnum.OWNER);
        owner.setOnline(true);
        room.addPlayer(owner);

        // 保存到 Redis 和内存缓存
        saveRoom(room);
        roomCache.putUserRoom(ownerId, roomId);

        // 缓存会话
        saveSession(ownerId, roomId, ownerName, ownerAvatar);

        log.info("创建游戏房间: roomId={}", roomId);

        // 广播房间新增消息给所有在线用户
        broadcastRoomAdded(room);

        return room;
    }

    /**
     * 加入房间（默认非创建者加入）
     */
    public GameRoom joinRoom(String roomId, Long userId, String userName, String userAvatar, String password) {
        return joinRoom(roomId, userId, userName, userAvatar, password, false);
    }

    /**
     * 加入房间
     * 支持断线重连
     * @param isCreatorJoin 是否是创建房间后首次加入（用于区分重连）
     */
    public GameRoom joinRoom(String roomId, Long userId, String userName, String userAvatar, String password, boolean isCreatorJoin) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            log.warn("房间不存在: roomId={}", roomId);
            return null;
        }

        GameSession cachedSession = getUserSession(userId);
        boolean isExistingPlayer = room.getPlayer(userId) != null;
        
        // 判断是否为真正的重连：玩家已在房间中但当前离线
        // 注意：创建房间后加入不视为重连，即使会话中记录了房间
        boolean isReconnecting = isExistingPlayer 
                && cachedSession != null 
                && !cachedSession.isOnline();

        // 只有真正的重连才校验重连窗口（创建后加入不校验）
        if (isReconnecting && !cachedSession.isWithinReconnectWindow()) {
            log.warn("重连窗口已过期: userId={}, roomId={}", userId, roomId);
            sessionCache.deleteSession(userId);
            isReconnecting = false;
        }

        // 非重连用户检查
        if (!isReconnecting) {
            // 检查是否已在其他房间
            String existingRoomId = roomCache.getUserRoomId(userId);
            if (existingRoomId != null) {
                if (existingRoomId.equals(roomId)) {
                    return room;
                }
                return null;
            }

            // 检查房间状态
            if (room.getState() == RoomStateEnum.CLOSED) {
                log.warn("房间已关闭: roomId={}", roomId);
                return null;
            }

            // 非等待状态不能加入（新玩家）
            if (room.getState() != RoomStateEnum.WAITING && room.getState() != RoomStateEnum.READY) {
                log.warn("房间不在等待状态，无法加入: roomId={}, state={}", roomId, room.getState());
                return null;
            }

            if (room.getPlayerCount() >= room.getMaxPlayers()) {
                log.warn("房间已满: roomId={}, maxPlayers={}", roomId, room.getMaxPlayers());
                return null;
            }

            // 密码检查
            if (room.isNeedPassword() && !room.verifyPassword(password)) {
                log.warn("房间密码错误: roomId={}", roomId);
                return null;
            }
        }

        // 获取或创建玩家
        GamePlayer player = room.getPlayer(userId);

        if (player == null) {
            // 新玩家加入
            player = new GamePlayer(userId, userName, userAvatar);
            player.setOnline(true);

            // 重连时检查是否在游戏中（玩家不存在说明之前已完全离开）
            if (room.getState().isPlaying()) {
                log.warn("游戏进行中，新玩家无法加入: roomId={}, userId={}", roomId, userId);
                return null;
            }

            if (!room.addPlayer(player)) {
                return null;
            }
        } else {
            // 重连：恢复玩家在线状态
            player.setOnline(true);
            player.setUserName(userName); // 更新用户名
            if (userAvatar != null) {
                player.setAvatar(userAvatar);
            }
        }

        // 更新映射
        roomCache.putUserRoom(userId, roomId);
        saveRoom(room);

        // 更新会话缓存
        saveSession(userId, roomId, userName, userAvatar);

        log.debug("用户加入房间: userId={}", userId);

        return room;
    }

    /**
     * 离开房间
     */
    public boolean leaveRoom(String roomId, Long userId) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            return false;
        }

        GamePlayer player = room.getPlayer(userId);
        if (player == null) {
            return false;
        }

        // 如果是游戏中，设置为离线（而不是移除）
        if (room.getState().isPlaying()) {
            player.setOnline(false);
            saveRoom(room);

            // 保存会话用于重连
            GameSession session = getUserSession(userId);
            if (session != null) {
                session.markOffline();
                sessionCache.saveSession(session);
            } else {
                saveSession(userId, roomId, player.getUserName(), player.getAvatar());
                GameSession newSession = getUserSession(userId);
                if (newSession != null) {
                    newSession.markOffline();
                    sessionCache.saveSession(newSession);
                }
            }

            log.info("用户在游戏中离开（设置离线）: roomId={}, userId={}", roomId, userId);

            // 检查是否所有玩家都离线了，如果是则删除房间
            if (room.getOnlinePlayerCount() == 0) {
                log.info("游戏中所有玩家都已离线，删除房间: roomId={}", roomId);
                removeRoom(roomId);
            }
            return true;
        }

        // 非游戏中，真正离开
        if (room.removePlayer(userId)) {
            roomCache.removeUserRoom(userId);
            sessionCache.deleteSession(userId);
            saveRoom(room);
            log.info("用户离开房间: roomId={}, userId={}", roomId, userId);

            // 如果房间空了，删除
            if (room.getPlayerCount() == 0) {
                removeRoom(roomId);
            }

            return true;
        }

        return false;
    }

    /**
     * 强制移除玩家（用于超时处理）
     */
    public boolean kickPlayer(String roomId, Long userId) {
        GameRoom room = getRoom(roomId);
        if (room == null) {
            return false;
        }

        GamePlayer player = room.getPlayer(userId);
        if (player == null) {
            return false;
        }

        roomCache.removeUserRoom(userId);
        sessionCache.deleteSession(userId);

        if (room.removePlayer(userId)) {
            saveRoom(room);
            log.info("移除离线玩家: roomId={}, userId={}", roomId, userId);

            // 如果房间空了，删除
            if (room.getPlayerCount() == 0) {
                removeRoom(roomId);
            }

            return true;
        }

        return false;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取房间（先从内存缓存，未命中从 Redis 读取）
     */
    public GameRoom getRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        // 先从内存缓存读取
        GameRoom room = roomMemoryCache.get(roomId);
        if (room != null) {
            return room;
        }
        // 从 Redis 读取
        room = roomCache.getRoom(roomId);
        if (room != null) {
            roomMemoryCache.put(roomId, room);
        }
        return room;
    }

    /**
     * 获取用户所在房间
     */
    public GameRoom getUserRoom(Long userId) {
        String roomId = roomCache.getUserRoomId(userId);
        return roomId != null ? getRoom(roomId) : null;
    }

    /**
     * 获取用户所在房间ID
     */
    public String getUserRoomId(Long userId) {
        return roomCache.getUserRoomId(userId);
    }

    /**
     * 获取用户会话
     */
    public GameSession getUserSession(Long userId) {
        return sessionCache.getSession(userId);
    }

    /**
     * 获取所有等待中的房间
     */
    public List<GameRoom> getWaitingRooms() {
        return roomCache.getAllRooms().stream()
                .filter(r -> r.getState() == RoomStateEnum.WAITING || r.getState() == RoomStateEnum.READY)
                .filter(r -> !r.isNeedPassword())
                .sorted(Comparator.comparing(GameRoom::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 按游戏类型获取房间列表
     */
    public List<GameRoom> getRoomsByType(GameTypeEnum gameType) {
        return roomCache.getRoomsByType(gameType).stream()
                .filter(r -> r.getState() == RoomStateEnum.WAITING
                        || r.getState() == RoomStateEnum.READY
                        || r.getState() == RoomStateEnum.ROBBING)
                .sorted(Comparator.comparing(GameRoom::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取房间列表
     */
    public List<GameRoom.RoomInfo> getRoomList(GameTypeEnum gameType) {
        List<GameRoom> roomList;
        if (gameType != null) {
            roomList = getRoomsByType(gameType);
        } else {
            roomList = getWaitingRooms();
        }

        return roomList.stream()
                .map(GameRoom::getInfo)
                .collect(Collectors.toList());
    }

    /**
     * 移除房间
     */
    public void removeRoom(String roomId) {
        GameRoom room = getRoom(roomId);
        if (room != null) {
            GameTypeEnum gameType = room.getGameType();

            // 清除所有玩家的房间映射和会话
            for (Long userId : room.getPlayerOrder()) {
                roomCache.removeUserRoom(userId);
                sessionCache.deleteSession(userId);
            }

            // 从 Redis 删除
            roomCache.deleteRoom(roomId);
            roomCache.removeRoomExpiry(roomId);

            // 清除内存缓存
            roomMemoryCache.remove(roomId);

            log.info("移除房间: roomId={}", roomId);

            // 广播房间删除消息给所有在线用户
            broadcastRoomRemoved(roomId, gameType);
        }
    }

    /**
     * 清理超时房间
     */
    public void cleanTimeoutRooms(long timeoutMs) {
        long now = System.currentTimeMillis();
        Set<String> roomIds = roomCache.getAllRoomIds();

        List<String> toRemove = new ArrayList<>();
        for (String roomId : roomIds) {
            GameRoom room = getRoom(roomId);
            if (room != null && room.getPlayerCount() == 0 && now - room.getLastActiveTime() > timeoutMs) {
                toRemove.add(roomId);
            }
        }

        for (String roomId : toRemove) {
            removeRoom(roomId);
        }


    /**
     * 清理离线超时的玩家
     */
    public void cleanOfflinePlayers() {
        long now = System.currentTimeMillis();

        List<GameSession> allSessions = sessionCache.getAllSessions();
        for (GameSession session : allSessions) {
            if (!session.isOnline() && session.getDisconnectedAt() > 0) {
                long offlineDuration = now - session.getDisconnectedAt();
                if (offlineDuration > OFFLINE_TIMEOUT_MS) {
                    String roomId = session.getRoomId();
                    if (roomId != null) {
                        kickPlayer(roomId, session.getUserId());
                    }
                }
            }
        }
    }

    /**
     * 获取统计数据
     */
    public RoomStats getStats() {
        List<GameRoom> allRooms = roomCache.getAllRooms();
        int totalRooms = allRooms.size();
        int waitingRooms = (int) allRooms.stream()
                .filter(r -> r.getState() == RoomStateEnum.WAITING)
                .count();
        int playingRooms = (int) allRooms.stream()
                .filter(r -> r.getState().isPlaying())
                .count();
        int totalPlayers = allRooms.stream()
                .mapToInt(GameRoom::getPlayerCount)
                .sum();

        return new RoomStats(totalRooms, waitingRooms, playingRooms, totalPlayers);
    }

    /**
     * 同步房间状态到 Redis（游戏过程中调用）
     */
    public void saveRoom(GameRoom room) {
        if (room == null || room.getRoomId() == null) {
            return;
        }
        roomCache.saveRoom(room);
        roomMemoryCache.put(room.getRoomId(), room);
    }

    // ==================== 私有方法 ====================

    /**
     * 保存会话到 Redis
     */
    public void saveSession(GameSession session) {
        if (session != null) {
            sessionCache.saveSession(session);
        }
    }

    /**
     * 生成房间ID
     */
    private String generateRoomId() {
        Random random = new Random();
        String roomId;
        int attempts = 0;

        do {
            roomId = String.format("%06d", random.nextInt(1000000));
            attempts++;
        } while (roomMemoryCache.containsKey(roomId) && attempts < 10);

        return roomId;
    }

    /**
     * 保存会话
     */
    private void saveSession(Long userId, String roomId, String userName, String avatar) {
        GameSession session = getUserSession(userId);
        if (session == null) {
            session = new GameSession();
        }
        session.setUserId(userId);
        session.setRoomId(roomId);
        session.setUserName(userName);
        session.setAvatar(avatar);
        session.setOnline(true);
        session.setLastHeartbeat(System.currentTimeMillis());
        sessionCache.saveSession(session);
    }

    /**
     * 广播房间新增消息给所有在线用户
     */
    private void broadcastRoomAdded(GameRoom room) {
        if (sessionManager != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("roomId", room.getRoomId());
            data.put("gameType", room.getGameType());
            data.put("playerCount", room.getPlayerCount());
            data.put("roomInfo", room.toRoomInfoResp());
            sessionManager.broadcastToAll(GameMessageTypeEnum.ROOM_ADDED.getType(), data);
            log.info("广播房间新增消息: roomId={}", room.getRoomId());
        }
    }

    /**
     * 广播房间删除消息给所有在线用户
     */
    private void broadcastRoomRemoved(String roomId, GameTypeEnum gameType) {
        if (sessionManager != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("roomId", roomId);
            data.put("gameType", gameType);
            sessionManager.broadcastToAll(GameMessageTypeEnum.ROOM_REMOVED.getType(), data);
            log.info("广播房间删除消息: roomId={}", roomId);
        }
    }

    /**
     * 房间统计
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RoomStats {
        private int totalRooms;
        private int waitingRooms;
        private int playingRooms;
        private int totalPlayers;
    }

    /**
     * 房间限制信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoomRestrictionInfo {
        private String roomId;
        private GameTypeEnum gameType;
        private RoomStateEnum state;
        private String reason;
    }

    // ==================== 房间限制相关 ====================

    /**
     * 检查用户是否有房间限制
     */
    public boolean hasRoomRestriction(Long userId) {
        GameSession session = sessionCache.getSession(userId);
        if (session == null || !session.hasTempLeave()) {
            return false;
        }

        // 检查原房间是否还存在
        String tempRoomId = session.getTempLeaveRoomId();
        GameRoom room = getRoom(tempRoomId);

        // 房间不存在，限制自动解除
        if (room == null) {
            session.clearTempLeave();
            sessionCache.saveSession(session);
            return false;
        }

        // 房间存在且游戏在进行中，有限制
        return room.getState().isPlaying();
    }

    /**
     * 获取用户当前房间限制信息
     */
    public RoomRestrictionInfo getRoomRestrictionInfo(Long userId) {
        GameSession session = sessionCache.getSession(userId);
        if (session == null || !session.hasTempLeave()) {
            return null;
        }

        String tempRoomId = session.getTempLeaveRoomId();
        GameRoom room = getRoom(tempRoomId);

        // 房间不存在，限制解除
        if (room == null) {
            session.clearTempLeave();
            sessionCache.saveSession(session);
            return null;
        }

        // 返回限制信息
        return RoomRestrictionInfo.builder()
                .roomId(tempRoomId)
                .gameType(room.getGameType())
                .state(room.getState())
                .reason(room.getState().isPlaying() ? "游戏进行中" : "等待中")
                .build();
    }
}