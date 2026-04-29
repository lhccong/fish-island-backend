package com.cong.fishisland.socketio;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHero;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHeroSkin;
import com.cong.fishisland.model.entity.fishbattle.FishBattleSummonerSpell;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.fishbattle.FishBattleHeroService;
import com.cong.fishisland.service.fishbattle.FishBattleHeroSkinService;
import com.cong.fishisland.service.fishbattle.FishBattleSummonerSpellService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

/**
 * 摸鱼大乱斗 Socket.IO 事件处理器。
 * 游戏大厅和房间相关数据全部走 Socket + 内存，游戏开始前不涉及任何DB操作。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FishBattleSocketHandler {

    private final SocketIOServer fishBattleSocketIOServer;
    private final FishBattleBroadcastService broadcastService;
    private final FishBattleRoomManager roomManager;
    private final FishBattleHeroService fishBattleHeroService;
    private final FishBattleHeroSkinService fishBattleHeroSkinService;
    private final FishBattleSummonerSpellService fishBattleSummonerSpellService;
    private final UserService userService;
    private final com.cong.fishisland.service.fishbattle.FishBattleUserStatsService fishBattleUserStatsService;
    private final com.cong.fishisland.service.fishbattle.FishBattleRoomService fishBattleRoomService;
    private final com.cong.fishisland.config.fishbattle.FishBattleServerProperties fishBattleServerProperties;
    private final com.cong.fishisland.socketio.battle.Battle3dRoomManager battle3dRoomManager;
    private final com.cong.fishisland.socketio.battle.InputQueue inputQueue;
    private final com.cong.fishisland.socketio.battle.Battle3dBroadcastService battle3dBroadcastService;
    private final ScheduledExecutorService heroPickScheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> heroPickTimers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 后端启动时清理 DB 中残留的进行中房间（后端重启后内存丢失，这些房间已无法恢复）
        try {
            int cleaned = fishBattleRoomService.cleanupStaleRooms();
            if (cleaned > 0) {
                log.info("后端启动：已清理 {} 个残留的进行中房间（标记为异常结束）", cleaned);
            }
        } catch (Exception e) {
            log.warn("清理残留房间异常: {}", e.getMessage());
        }

        // 注册全员离线延迟销毁回调
        roomManager.setRoomDestroyedCallback((roomCode, endedDbRoomId) -> {
            broadcastLobbyRoomRemoved(roomCode);
            syncEndedRoomToDb(endedDbRoomId, roomCode);
            broadcastLobbyOverview();
        });

        /* 连接/断连生命周期 */
        fishBattleSocketIOServer.addConnectListener(this::onConnect);
        fishBattleSocketIOServer.addDisconnectListener(this::onDisconnect);

        /* ==================== 大厅事件 ==================== */
        fishBattleSocketIOServer.addEventListener("lobby:join", JsonNode.class, (client, data, ackRequest) -> {
            handleLobbyJoin(client);
        });
        fishBattleSocketIOServer.addEventListener("lobby:leave", JsonNode.class, (client, data, ackRequest) -> {
            handleLobbyLeave(client);
        });
        fishBattleSocketIOServer.addEventListener("lobby:getRooms", JsonNode.class, (client, data, ackRequest) -> {
            handleLobbyGetRooms(client);
        });
        fishBattleSocketIOServer.addEventListener("lobby:getOverview", JsonNode.class, (client, data, ackRequest) -> {
            handleLobbyGetOverview(client);
        });

        /* ==================== 房间操作事件 ==================== */
        fishBattleSocketIOServer.addEventListener("room:create", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomCreate(client, data);
        });
        fishBattleSocketIOServer.addEventListener("room:quickMatch", JsonNode.class, (client, data, ackRequest) -> {
            handleQuickMatch(client);
        });
        fishBattleSocketIOServer.addEventListener("room:getInfo", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomGetInfo(client, data);
        });
        fishBattleSocketIOServer.addEventListener("room:join", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomJoin(client, data);
        });
        fishBattleSocketIOServer.addEventListener("room:leave", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomLeave(client);
        });
        fishBattleSocketIOServer.addEventListener("room:switchSlot", JsonNode.class, (client, data, ackRequest) -> {
            handleSwitchSlot(client, data);
        });
        fishBattleSocketIOServer.addEventListener("room:ready", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomReady(client, data);
        });
        fishBattleSocketIOServer.addEventListener("room:chat", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomChat(client, data);
        });
        fishBattleSocketIOServer.addEventListener("room:rejoin", JsonNode.class, (client, data, ackRequest) -> {
            handleRoomRejoin(client, data);
        });

        /* 游戏开始（房主触发）→ 进入选英雄阶段 */
        fishBattleSocketIOServer.addEventListener("game:start", JsonNode.class, (client, data, ackRequest) -> {
            handleGameStart(client);
        });

        /* 选英雄事件 */
        fishBattleSocketIOServer.addEventListener("hero:select", JsonNode.class, (client, data, ackRequest) -> {
            handleHeroSelect(client, data);
        });
        fishBattleSocketIOServer.addEventListener("hero:confirm", JsonNode.class, (client, data, ackRequest) -> {
            handleHeroConfirm(client);
        });
        fishBattleSocketIOServer.addEventListener("hero:unconfirm", JsonNode.class, (client, data, ackRequest) -> {
            handleHeroUnconfirm(client);
        });
        fishBattleSocketIOServer.addEventListener("hero:selectSkin", JsonNode.class, (client, data, ackRequest) -> {
            handleHeroSelectSkin(client, data);
        });
        fishBattleSocketIOServer.addEventListener("hero:selectSpells", JsonNode.class, (client, data, ackRequest) -> {
            handleHeroSelectSpells(client, data);
        });

        /* 游戏加载完成 */
        fishBattleSocketIOServer.addEventListener("game:loadingProgress", JsonNode.class, (client, data, ackRequest) -> {
            handleGameLoadingProgress(client, data);
        });
        fishBattleSocketIOServer.addEventListener("game:loaded", JsonNode.class, (client, data, ackRequest) -> {
            handleGameLoaded(client, data);
        });

        /* ==================== 对战事件 ==================== */
        fishBattleSocketIOServer.addEventListener("battle:join", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleJoin(client, data);
        });
        fishBattleSocketIOServer.addEventListener("battle:sceneReady", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleSceneReady(client, data);
        });
        fishBattleSocketIOServer.addEventListener("battle:ping", JsonNode.class, (client, data, ackRequest) -> {
            handleBattlePing(client, data, ackRequest);
        });

        /* ==================== 对战移动事件 ==================== */
        fishBattleSocketIOServer.addEventListener("champion:move", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleMove(client, data);
        });
        fishBattleSocketIOServer.addEventListener("move", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleMove(client, data);
        });
        fishBattleSocketIOServer.addEventListener("champion:stop", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleStop(client, data);
        });
        fishBattleSocketIOServer.addEventListener("stop", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleStop(client, data);
        });
        fishBattleSocketIOServer.addEventListener("castSpell", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleCastSpell(client, data);
        });
        fishBattleSocketIOServer.addEventListener("basicAttack", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleBasicAttack(client, data);
        });

        /* ==================== 表情/语音/动画广播中继 ==================== */
        fishBattleSocketIOServer.addEventListener("champion:animate", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleBroadcastRelay(client, "champion:animate", data);
        });
        fishBattleSocketIOServer.addEventListener("champion:emote", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleBroadcastRelay(client, "champion:emote", data);
        });
        fishBattleSocketIOServer.addEventListener("champion:voice", JsonNode.class, (client, data, ackRequest) -> {
            handleBattleBroadcastRelay(client, "champion:voice", data);
        });

        /* 启动服务器 */
        fishBattleSocketIOServer.start();
        log.info("摸鱼大乱斗 Socket.IO 服务器启动成功，所有事件监听器已注册");
    }

    /* ==================== 连接生命周期 ==================== */

    private void onConnect(SocketIOClient client) {
        // 从握手参数解析已认证的 userId 并绑定到 session 属性
        String tokenValue = client.getHandshakeData().getSingleUrlParam("tokenValue");
        if (tokenValue != null && !tokenValue.isEmpty()) {
            try {
                Object loginId = StpUtil.getLoginIdByToken(tokenValue);
                if (loginId != null) {
                    long userId = Long.parseLong(loginId.toString());
                    client.set("authUserId", userId);
                    roomManager.addConnectedUser(client.getSessionId().toString(), userId, client);
                    log.info("摸鱼大乱斗 Socket.IO 客户端已连接，sessionId={}, authUserId={}", client.getSessionId(), userId);
                    return;
                }
            } catch (Exception e) {
                log.warn("Socket.IO onConnect 解析 userId 失败: {}", e.getMessage());
            }
        }
        // 鉴权应在 AuthorizationListener 中完成，此处为防御性断开
        log.warn("Socket.IO 客户端鉴权失败，强制断开: sessionId={}", client.getSessionId());
        client.disconnect();
    }

    /**
     * 从 Socket session 属性中获取已认证的 userId（不信任前端payload）
     */
    private Long getAuthUserId(SocketIOClient client) {
        Object val = client.get("authUserId");
        if (val instanceof Long) {
            return (Long) val;
        }
        return null;
    }

    private void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();

        // 从全局连接跟踪中移除（确保 getOnlineCount 立即减少）
        roomManager.removeConnectedUser(sessionId);

        // 从战斗房间中移除玩家会话
        battle3dRoomManager.removeSessionById(sessionId);

        // 尝试从房间移除
        FishBattleRoomManager.LeaveResult result = roomManager.leaveRoom(client);
        if (result != null) {
            FishBattleRoomManager.PlayerConnection conn = result.getLeavingPlayer();
            String roomCode = conn.getRoomCode();

            if (result.isDisconnectedInGame()) {
                // 对局中断连：广播玩家列表快照（含在线状态），不广播 playerLeft
                broadcastPlayersSnapshot(roomCode);
                // 如果全员离线导致房间自动结束，通知大厅移除该房间 + 同步DB
                if (result.isRoomRemoved()) {
                    broadcastLobbyRoomRemoved(roomCode);
                    syncEndedRoomToDb(result.getEndedDbRoomId(), roomCode);
                }
            } else {
                // 等待中离开：广播玩家离开 + 最新玩家列表
                broadcastPlayerLeft(conn);
                broadcastPlayersSnapshot(roomCode);

                // 如果房主转移了，广播房主变更
                if (result.getNewOwnerId() != null) {
                    broadcastOwnerChanged(roomCode, result.getNewOwnerId(), result.getNewOwnerName());
                }

                // 通知大厅：房间更新或移除
                if (result.isRoomRemoved()) {
                    broadcastLobbyRoomRemoved(roomCode);
                } else {
                    broadcastLobbyRoomUpdated(roomCode);
                }
            }
        }

        // 广播最新概览给所有已连接客户端（在线人数变化）
        broadcastLobbyOverview();

        log.info("摸鱼大乱斗 Socket.IO 客户端已断开，sessionId={}", sessionId);
    }

    /* ==================== 大厅操作 ==================== */

    private void handleLobbyJoin(SocketIOClient client) {
        log.debug("客户端加入大厅, sessionId={}", client.getSessionId());
        // 广播最新概览给所有已连接客户端
        broadcastLobbyOverview();
    }

    private void handleLobbyLeave(SocketIOClient client) {
        log.debug("客户端离开大厅, sessionId={}", client.getSessionId());
    }

    private void handleLobbyGetRooms(SocketIOClient client) {
        // 所有房间数据统一从内存获取（内存是唯一可信数据源）
        List<Map<String, Object>> allRooms = roomManager.getAllActiveRoomsSnapshot();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rooms", allRooms);
        payload.put("total", allRooms.size());
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "lobby:roomList", payload);
    }

    private void handleLobbyGetOverview(SocketIOClient client) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("onlineCount", roomManager.getOnlineCount());
        payload.put("roomCount", roomManager.getActiveRoomCount());
        payload.put("fightingCount", roomManager.getFightingPlayerCount());
        payload.put("fightingPlayers", roomManager.getFightingPlayersSnapshot());
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "lobby:overview", payload);
    }

    private void broadcastLobbyOverview() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("onlineCount", roomManager.getOnlineCount());
        payload.put("roomCount", roomManager.getActiveRoomCount());
        payload.put("fightingCount", roomManager.getFightingPlayerCount());
        payload.put("fightingPlayers", roomManager.getFightingPlayersSnapshot());
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomManager.getAllConnectedClients(), "lobby:overview", payload);
    }

    private void broadcastLobbyRoomCreated(String roomCode) {
        Map<String, Object> room = roomManager.getRoomInfoSnapshot(roomCode);
        if (room == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("room", room);
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomManager.getAllConnectedClients(), "lobby:roomCreated", payload);
    }

    private void broadcastLobbyRoomUpdated(String roomCode) {
        Map<String, Object> room = roomManager.getRoomInfoSnapshot(roomCode);
        if (room == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("room", room);
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomManager.getAllConnectedClients(), "lobby:roomUpdated", payload);
    }

    private void broadcastLobbyRoomRemoved(String roomCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roomCode", roomCode);
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomManager.getAllConnectedClients(), "lobby:roomRemoved", payload);
    }

    /* ==================== 房间操作 ==================== */

    private void handleRoomCreate(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            sendError(client, "参数不完整");
            return;
        }
        // 从已认证的 session 属性获取 userId，不信任前端 payload
        Long userId = getAuthUserId(client);
        if (userId == null) {
            sendError(client, "请先登录");
            return;
        }

        // 检查每日对局上限
        if (fishBattleUserStatsService.isDailyLimitReached(userId)) {
            sendError(client, "今日对局次数已达上限（20局），明天再来吧！");
            return;
        }

        // 防重复：检查该用户是否已在其他房间中
        String existingRoom = roomManager.findRoomByUserId(userId);
        if (existingRoom != null) {
            sendErrorWithRoom(client, buildExistingRoomMessage(existingRoom), existingRoom);
            return;
        }

        User currentUser = userService.getById(userId);
        String roomName = payload.path("roomName").asText("摸鱼大乱斗房间");
        String gameMode = payload.path("gameMode").asText("classic");
        boolean aiFillEnabled = payload.path("aiFillEnabled").asBoolean(true);
        String userName = currentUser != null && currentUser.getUserName() != null
                ? currentUser.getUserName()
                : payload.path("userName").asText("未知玩家");

        // 纯内存创建房间
        FishBattleRoomManager.RoomSession session = roomManager.createRoom(
                roomName, gameMode, aiFillEnabled, userId, userName);

        // 回传创建成功（包含 roomCode，前端跳转用）
        Map<String, Object> createdPayload = new LinkedHashMap<>();
        createdPayload.put("roomCode", session.getRoomCode());
        createdPayload.put("roomName", session.getRoomName());
        createdPayload.put("gameMode", session.getGameMode());
        createdPayload.put("maxPlayers", session.getMaxPlayers());
        createdPayload.put("aiFillEnabled", session.isAiFillEnabled() ? 1 : 0);
        createdPayload.put("creatorId", session.getCreatorId());
        createdPayload.put("creatorName", session.getCreatorName());
        createdPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "room:created", createdPayload);

        // 广播给大厅所有人：新房间创建 + 概览更新（房间数变化）
        broadcastLobbyRoomCreated(session.getRoomCode());
        broadcastLobbyOverview();
    }

    private void handleQuickMatch(SocketIOClient client) {
        Long userId = getAuthUserId(client);
        if (userId == null) {
            sendError(client, "请先登录");
            return;
        }

        // 检查每日对局上限
        if (fishBattleUserStatsService.isDailyLimitReached(userId)) {
            sendError(client, "今日对局次数已达上限（20局），明天再来吧！");
            return;
        }

        // 检查是否已在房间
        String existingRoom = roomManager.findRoomByUserId(userId);
        if (existingRoom != null) {
            sendErrorWithRoom(client, buildExistingRoomMessage(existingRoom), existingRoom);
            return;
        }

        User currentUser = userService.getById(userId);
        String playerName = currentUser != null && currentUser.getUserName() != null
                ? currentUser.getUserName() : "未知玩家";
        String userAvatar = currentUser != null ? currentUser.getUserAvatar() : null;

        // 尝试找到一个有空位的等待中房间
        String availableRoom = roomManager.findAvailableRoom();
        if (availableRoom != null) {
            // 自动分配队伍和位置
            long blueCount = roomManager.getTeamCount(availableRoom, "blue");
            long redCount = roomManager.getTeamCount(availableRoom, "red");
            String team = blueCount <= redCount ? "blue" : "red";
            int slotIndex = findFirstEmptySlot(availableRoom, team);

            FishBattleRoomManager.PlayerConnection conn = roomManager.joinRoom(
                    availableRoom, userId, playerName, userAvatar, team, slotIndex, client);
            if (conn != null) {
                // 回传匹配成功（前端跳转到该房间）
                Map<String, Object> matchedPayload = new LinkedHashMap<>();
                matchedPayload.put("roomCode", availableRoom);
                matchedPayload.put("matched", true);
                matchedPayload.put("serverTime", System.currentTimeMillis());
                broadcastService.sendToClient(client, "room:matched", matchedPayload);

                // 广播新玩家加入
                Collection<SocketIOClient> roomClients = roomManager.getRoomClients(availableRoom);
                Map<String, Object> broadcastPayload = new LinkedHashMap<>();
                broadcastPayload.put("userId", userId);
                broadcastPayload.put("playerName", playerName);
                broadcastPayload.put("serverTime", System.currentTimeMillis());
                broadcastService.broadcast(roomClients, "room:playerJoined", broadcastPayload);
                broadcastPlayersSnapshot(availableRoom);
                broadcastLobbyRoomUpdated(availableRoom);
                broadcastLobbyOverview();
                return;
            }
        }

        // 没有可用房间，创建新房间
        FishBattleRoomManager.RoomSession session = roomManager.createRoom(
                "快速匹配房间", "classic", true, userId, playerName);

        Map<String, Object> createdPayload = new LinkedHashMap<>();
        createdPayload.put("roomCode", session.getRoomCode());
        createdPayload.put("roomName", session.getRoomName());
        createdPayload.put("gameMode", session.getGameMode());
        createdPayload.put("maxPlayers", session.getMaxPlayers());
        createdPayload.put("aiFillEnabled", session.isAiFillEnabled() ? 1 : 0);
        createdPayload.put("creatorId", session.getCreatorId());
        createdPayload.put("creatorName", session.getCreatorName());
        createdPayload.put("matched", false);
        createdPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "room:matched", createdPayload);

        broadcastLobbyRoomCreated(session.getRoomCode());
        broadcastLobbyOverview();
    }

    private void handleRoomGetInfo(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            sendError(client, "参数不完整");
            return;
        }
        String roomCode = payload.path("roomCode").asText(null);
        if (roomCode == null) {
            sendError(client, "房间编码不能为空");
            return;
        }

        Map<String, Object> roomInfo = roomManager.getRoomInfoSnapshot(roomCode);
        if (roomInfo == null) {
            sendError(client, "房间不存在或已关闭");
            return;
        }

        List<Map<String, Object>> players = roomManager.getRoomPlayersSnapshot(roomCode);

        Map<String, Object> infoPayload = new LinkedHashMap<>();
        infoPayload.put("room", roomInfo);
        infoPayload.put("players", players);
        infoPayload.put("serverTime", System.currentTimeMillis());

        // 如果处于选英雄或加载阶段，额外返回英雄列表、技能元数据和倒计时/加载摘要
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        // 召唤师技能列表 — 始终返回（Loading 页断线重连也需要）
        List<FishBattleSummonerSpell> spells = fishBattleSummonerSpellService.listEnabledSpells();
        List<Map<String, Object>> spellList = new ArrayList<>();
        for (FishBattleSummonerSpell spell : spells) {
            Map<String, Object> spellItem = new LinkedHashMap<>();
            spellItem.put("spellId", spell.getSpellId());
            spellItem.put("name", spell.getName());
            spellItem.put("icon", spell.getIcon());
            spellItem.put("description", spell.getDescription());
            spellItem.put("cooldown", spell.getCooldown());
            spellItem.put("assetConfig", spell.getAssetConfig());
            spellList.add(spellItem);
        }
        infoPayload.put("summonerSpells", spellList);

        if (session != null && (session.isHeroPickPhase() || session.isLoadingPhase())) {
            List<FishBattleHero> heroes = fishBattleHeroService.listEnabledHeroes();
            List<Map<String, Object>> heroList = new ArrayList<>();
            for (FishBattleHero hero : heroes) {
                Map<String, Object> heroItem = new LinkedHashMap<>();
                heroItem.put("heroId", hero.getHeroId());
                heroItem.put("name", hero.getName());
                heroItem.put("nameEn", hero.getNameEn());
                heroItem.put("role", hero.getRole());
                heroItem.put("baseHp", hero.getBaseHp());
                heroItem.put("baseMp", hero.getBaseMp());
                heroItem.put("baseAd", hero.getBaseAd());
                heroItem.put("avatarUrl", hero.getAvatarUrl());
                heroItem.put("splashArt", hero.getSplashArt());
                heroItem.put("modelUrl", hero.getModelUrl());
                heroItem.put("assetConfig", hero.getAssetConfig());
                heroItem.put("skills", hero.getSkills());
                heroList.add(heroItem);
            }
            infoPayload.put("heroes", heroList);
            long remainMs = Math.max(0, session.getHeroPickDeadline() - System.currentTimeMillis());
            infoPayload.put("heroPickRemainSeconds", (int) (remainMs / 1000));
            infoPayload.put("heroPickDuration", fishBattleServerProperties.getHeroPickDuration());
            infoPayload.put("heroPickPlayers", roomManager.getHeroPickSnapshot(roomCode));

            // 英雄皮肤（按英雄分组）
            Map<String, List<Map<String, Object>>> skinMap = new LinkedHashMap<>();
            for (FishBattleHero hero : heroes) {
                List<FishBattleHeroSkin> skins = fishBattleHeroSkinService.listByHeroId(hero.getHeroId());
                List<Map<String, Object>> skinItems = new ArrayList<>();
                for (FishBattleHeroSkin skin : skins) {
                    Map<String, Object> skinItem = new LinkedHashMap<>();
                    skinItem.put("skinId", skin.getSkinId());
                    skinItem.put("skinName", skin.getSkinName());
                    skinItem.put("splashArt", skin.getSplashArt());
                    skinItem.put("modelUrl", skin.getModelUrl());
                    skinItem.put("isDefault", skin.getIsDefault());
                    skinItems.add(skinItem);
                }
                skinMap.put(hero.getHeroId(), skinItems);
            }
            infoPayload.put("heroSkins", skinMap);
            if (session.isLoadingPhase()) {
                infoPayload.put("loadedCount", roomManager.getLoadedPlayerCount(roomCode));
                infoPayload.put("totalPlayers", players.size());
            }
        }

        broadcastService.sendToClient(client, "room:info", infoPayload);
    }

    private void handleRoomJoin(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        // 从已认证的 session 属性获取 userId，不信任前端 payload
        Long userId = getAuthUserId(client);
        if (userId == null) {
            sendError(client, "请先登录");
            return;
        }
        User currentUser = userService.getById(userId);
        String roomCode = payload.path("roomCode").asText(null);
        String playerName = currentUser != null && currentUser.getUserName() != null
                ? currentUser.getUserName()
                : payload.path("playerName").asText("未知玩家");
        String userAvatar = currentUser != null ? currentUser.getUserAvatar() : null;
        String team = payload.path("team").asText(null);
        int slotIndex = payload.path("slotIndex").asInt(-1);

        if (roomCode == null) {
            sendError(client, "参数不完整");
            return;
        }

        // 检查每日对局上限
        if (fishBattleUserStatsService.isDailyLimitReached(userId)) {
            sendError(client, "今日对局次数已达上限（20局），明天再来吧！");
            return;
        }

        // 检查房间是否存在
        if (!roomManager.roomExists(roomCode)) {
            sendError(client, "房间不存在或已关闭");
            return;
        }

        // 检查房间状态：只有等待中(0)才允许加入
        FishBattleRoomManager.RoomSession roomSession = roomManager.getRoomSession(roomCode);
        if (roomSession != null && roomSession.getStatus() != 0) {
            String statusText;
            switch (roomSession.getStatus()) {
                case 1: statusText = "选英雄中"; break;
                case 2: statusText = "对局中"; break;
                default: statusText = "不可用"; break;
            }
            sendError(client, "房间当前" + statusText + "，无法加入");
            return;
        }

        // 防重复：检查用户是否已在其他房间中（同一房间的重复加入由 joinRoom 内部处理为 session 替换）
        String existingRoom = roomManager.findRoomByUserId(userId);
        if (existingRoom != null && !existingRoom.equals(roomCode)) {
            sendErrorWithRoom(client, buildExistingRoomMessage(existingRoom), existingRoom);
            return;
        }

        // 如果前端没指定team/slot，自动分配到人少的队伍第一个空位
        if (team == null || slotIndex < 0) {
            long blueCount = roomManager.getTeamCount(roomCode, "blue");
            long redCount = roomManager.getTeamCount(roomCode, "red");
            team = blueCount <= redCount ? "blue" : "red";
            slotIndex = findFirstEmptySlot(roomCode, team);
        }

        // joinRoom 内部已有 synchronized + 位置/队伍校验，外层不再重复检查（避免 check-then-act 竞态）
        // 纯内存加入房间
        FishBattleRoomManager.PlayerConnection conn = roomManager.joinRoom(roomCode, userId, playerName, userAvatar, team, slotIndex, client);
        if (conn == null) {
            sendError(client, "加入房间失败");
            return;
        }

        // 回传加入成功
        Map<String, Object> joinedPayload = new LinkedHashMap<>();
        joinedPayload.put("roomCode", roomCode);
        joinedPayload.put("userId", userId);
        joinedPayload.put("playerName", playerName);
        joinedPayload.put("team", team);
        joinedPayload.put("slotIndex", slotIndex);
        joinedPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "room:joined", joinedPayload);

        // 广播新玩家加入 + 最新玩家列表
        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> broadcastPayload = new LinkedHashMap<>();
        broadcastPayload.put("userId", userId);
        broadcastPayload.put("playerName", playerName);
        broadcastPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "room:playerJoined", broadcastPayload);
        broadcastPlayersSnapshot(roomCode);

        // 通知大厅：房间人数变化
        broadcastLobbyRoomUpdated(roomCode);
        broadcastLobbyOverview();
    }

    private void handleRoomLeave(SocketIOClient client) {
        FishBattleRoomManager.LeaveResult result = roomManager.leaveRoom(client);
        if (result != null) {
            FishBattleRoomManager.PlayerConnection conn = result.getLeavingPlayer();
            String roomCode = conn.getRoomCode();

            // 给离开的玩家发确认，确保前端在收到后再跳转
            Map<String, Object> leftPayload = new LinkedHashMap<>();
            leftPayload.put("roomCode", roomCode);
            leftPayload.put("serverTime", System.currentTimeMillis());
            broadcastService.sendToClient(client, "room:left", leftPayload);

            if (result.isDisconnectedInGame()) {
                // 对局中断连：广播玩家列表快照（含在线状态）
                broadcastPlayersSnapshot(roomCode);
                // 如果全员离线导致房间自动结束，通知大厅移除该房间 + 同步DB
                if (result.isRoomRemoved()) {
                    broadcastLobbyRoomRemoved(roomCode);
                    syncEndedRoomToDb(result.getEndedDbRoomId(), roomCode);
                }
            } else {
                // 等待中离开：广播玩家离开 + 最新玩家列表
                broadcastPlayerLeft(conn);
                broadcastPlayersSnapshot(roomCode);

                // 如果房主转移了，广播房主变更
                if (result.getNewOwnerId() != null) {
                    broadcastOwnerChanged(roomCode, result.getNewOwnerId(), result.getNewOwnerName());
                }

                // 通知大厅：房间更新或移除
                if (result.isRoomRemoved()) {
                    broadcastLobbyRoomRemoved(roomCode);
                } else {
                    broadcastLobbyRoomUpdated(roomCode);
                }
            }

            broadcastLobbyOverview();

        }
    }


    private void handleSwitchSlot(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        String newTeam = payload.path("team").asText(null);
        int newSlotIndex = payload.path("slotIndex").asInt(-1);

        if (newTeam == null || newSlotIndex < 0) {
            sendError(client, "参数不完整");
            return;
        }

        boolean success = roomManager.switchSlot(sessionId, newTeam, newSlotIndex);
        if (!success) {
            sendError(client, "切换失败，该位置已被占用或队伍已满");
            return;
        }

        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn != null) {
            broadcastPlayersSnapshot(conn.getRoomCode());
        }
    }

    private void handleRoomReady(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        boolean isReady = payload.path("isReady").asBoolean(false);

        boolean success = roomManager.setReady(sessionId, isReady);
        if (!success) {
            return;
        }

        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn != null) {
            broadcastPlayersSnapshot(conn.getRoomCode());
        }
    }

    private void handleRoomChat(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            return;
        }

        String message = payload.path("message").asText("");
        String roomCode = conn.getRoomCode();

        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> chatPayload = new LinkedHashMap<>();
        chatPayload.put("userId", conn.getUserId());
        chatPayload.put("playerName", conn.getPlayerName());
        chatPayload.put("message", message);
        chatPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "room:chatMessage", chatPayload);
    }

    private void handleRoomRejoin(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            sendError(client, "参数不完整");
            return;
        }
        Long userId = getAuthUserId(client);
        if (userId == null) {
            sendError(client, "请先登录");
            return;
        }
        String roomCode = payload.path("roomCode").asText(null);
        if (roomCode == null) {
            sendError(client, "房间编码不能为空");
            return;
        }

        if (!roomManager.roomExists(roomCode)) {
            sendError(client, "房间不存在或已关闭");
            return;
        }

        FishBattleRoomManager.PlayerConnection conn = roomManager.rejoinRoom(roomCode, userId, client);
        if (conn == null) {
            sendError(client, "你不在此房间中，返回大厅");
            return;
        }

        Map<String, Object> rejoinPayload = new LinkedHashMap<>();
        rejoinPayload.put("roomCode", roomCode);
        rejoinPayload.put("userId", userId);
        rejoinPayload.put("playerName", conn.getPlayerName());
        rejoinPayload.put("team", conn.getTeam());
        rejoinPayload.put("slotIndex", conn.getSlotIndex());
        rejoinPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "room:rejoined", rejoinPayload);

        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> broadcastPayload = new LinkedHashMap<>();
        broadcastPayload.put("userId", userId);
        broadcastPayload.put("playerName", conn.getPlayerName());
        broadcastPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "room:playerRejoined", broadcastPayload);
        broadcastPlayersSnapshot(roomCode);
        broadcastLobbyOverview();

        log.info("玩家重连成功: roomCode={}, userId={}", roomCode, userId);
    }

    private void handleGameStart(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            sendError(client, "您不在任何房间中");
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null) {
            sendError(client, "房间不存在");
            return;
        }
        if (!conn.getUserId().equals(session.getCreatorId())) {
            sendError(client, "只有房主才能开始游戏");
            return;
        }
        if (!roomManager.canStartGame(roomCode)) {
            sendError(client, "两队各需至少1人且全部准备就绪才能开始游戏");
            return;
        }

        List<FishBattleHero> heroes = fishBattleHeroService.listEnabledHeroes();
        if (heroes.isEmpty()) {
            sendError(client, "英雄池为空，无法开始游戏");
            return;
        }

        int heroPickDuration = fishBattleServerProperties.getHeroPickDuration();
        roomManager.startHeroPick(roomCode, heroPickDuration);

        try {
            List<Map<String, Object>> playerDataList = new ArrayList<>();
            for (FishBattleRoomManager.PlayerConnection p : session.getPlayers().values()) {
                Map<String, Object> pd = new LinkedHashMap<>();
                pd.put("userId", p.getUserId());
                pd.put("playerName", p.getPlayerName());
                pd.put("team", p.getTeam());
                pd.put("slotIndex", p.getSlotIndex());
                playerDataList.add(pd);
            }
            Long dbRoomId = fishBattleRoomService.persistRoomOnGameStart(
                    roomCode, session.getRoomName(), session.getGameMode(),
                    session.getMaxPlayers(), session.getPlayers().size(),
                    session.isAiFillEnabled(), session.getCreatorId(), playerDataList);
            session.setDbRoomId(dbRoomId);
            log.info("游戏开始落库成功: roomCode={}, dbRoomId={}", roomCode, dbRoomId);
        } catch (Exception e) {
            log.error("游戏开始落库异常: roomCode={}", roomCode, e);
        }

        List<Map<String, Object>> heroList = new ArrayList<>();
        for (FishBattleHero hero : heroes) {
            Map<String, Object> heroItem = new LinkedHashMap<>();
            heroItem.put("heroId", hero.getHeroId());
            heroItem.put("name", hero.getName());
            heroItem.put("nameEn", hero.getNameEn());
            heroItem.put("role", hero.getRole());
            heroItem.put("baseHp", hero.getBaseHp());
            heroItem.put("baseMp", hero.getBaseMp());
            heroItem.put("baseAd", hero.getBaseAd());
            heroItem.put("avatarUrl", hero.getAvatarUrl());
            heroItem.put("splashArt", hero.getSplashArt());
            heroItem.put("modelUrl", hero.getModelUrl());
            heroItem.put("assetConfig", hero.getAssetConfig());
            heroItem.put("skills", hero.getSkills());
            heroList.add(heroItem);
        }

        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> pickStartPayload = new LinkedHashMap<>();
        pickStartPayload.put("roomCode", roomCode);
        pickStartPayload.put("heroes", heroList);
        pickStartPayload.put("duration", heroPickDuration);
        pickStartPayload.put("players", roomManager.getHeroPickSnapshot(roomCode));
        pickStartPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "game:heroPickStart", pickStartPayload);

        broadcastLobbyRoomUpdated(roomCode);
        broadcastLobbyOverview();

        ScheduledFuture<?> timer = heroPickScheduler.schedule(() -> {
            try {
                handleHeroPickTimeout(roomCode, heroes);
            } catch (Exception e) {
                log.error("选英雄倒计时处理异常: roomCode={}", roomCode, e);
            }
        }, heroPickDuration, TimeUnit.SECONDS);
        heroPickTimers.put(roomCode, timer);
    }

    private void handleHeroSelect(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            sendError(client, "您不在任何房间中");
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || !session.isHeroPickPhase()) {
            sendError(client, "当前不在选英雄阶段");
            return;
        }

        String heroId = payload.path("heroId").asText(null);
        String heroName = payload.path("heroName").asText(null);
        String heroAvatarUrl = payload.path("heroAvatarUrl").asText(null);
        String heroSplashArt = payload.path("heroSplashArt").asText(null);
        String heroRole = payload.path("heroRole").asText(null);

        if (heroId == null) {
            sendError(client, "请选择一个英雄");
            return;
        }

        // 通过 heroId 查找英雄的 modelUrl（前端未传递此字段）
        String heroModelUrl = null;
        List<FishBattleHero> allHeroes = fishBattleHeroService.listEnabledHeroes();
        for (FishBattleHero h : allHeroes) {
            if (heroId.equals(h.getHeroId())) {
                heroModelUrl = h.getModelUrl();
                break;
            }
        }

        boolean success = roomManager.selectHero(sessionId, heroId, heroName,
                heroAvatarUrl, heroSplashArt, heroRole, heroModelUrl);
        if (!success) {
            sendError(client, "英雄已锁定，无法更换");
            return;
        }

        broadcastHeroPickUpdate(roomCode);
    }

    private void handleHeroConfirm(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            sendError(client, "您不在任何房间中");
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || !session.isHeroPickPhase()) {
            sendError(client, "当前不在选英雄阶段");
            return;
        }

        if (conn.getSelectedHeroId() == null) {
            sendError(client, "请先选择一个英雄");
            return;
        }

        boolean success = roomManager.confirmHero(sessionId);
        if (!success) {
            sendError(client, "确认英雄失败");
            return;
        }

        broadcastHeroPickUpdate(roomCode);

        if (roomManager.isAllHeroesConfirmed(roomCode)) {
            completeHeroPick(roomCode);
        }
    }

    private void handleHeroUnconfirm(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            sendError(client, "您不在任何房间中");
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || !session.isHeroPickPhase()) {
            sendError(client, "当前不在选英雄阶段");
            return;
        }

        boolean success = roomManager.unconfirmHero(sessionId);
        if (!success) {
            sendError(client, "取消锁定失败");
            return;
        }

        broadcastHeroPickUpdate(roomCode);
    }

    private void handleHeroPickTimeout(String roomCode, List<FishBattleHero> heroes) {
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || !session.isHeroPickPhase()) {
            return;
        }

        Random random = new Random();
        for (FishBattleRoomManager.PlayerConnection p : session.getPlayers().values()) {
            if (!p.isHeroConfirmed()) {
                FishBattleHero randomHero = heroes.get(random.nextInt(heroes.size()));
                p.setSelectedHeroId(randomHero.getHeroId());
                p.setHeroName(randomHero.getName());
                p.setHeroAvatarUrl(randomHero.getAvatarUrl());
                p.setHeroSplashArt(randomHero.getSplashArt());
                p.setHeroRole(randomHero.getRole());
                p.setHeroModelUrl(randomHero.getModelUrl());
                p.setHeroConfirmed(true);
            }
        }

        completeHeroPick(roomCode);
    }

    private void completeHeroPick(String roomCode) {
        ScheduledFuture<?> timer = heroPickTimers.remove(roomCode);
        if (timer != null) {
            timer.cancel(false);
        }

        roomManager.finishHeroPick(roomCode);

        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session != null && session.getDbRoomId() != null) {
            try {
                List<Map<String, Object>> heroDataList = new ArrayList<>();
                for (FishBattleRoomManager.PlayerConnection p : session.getPlayers().values()) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("userId", p.getUserId());
                    data.put("heroId", p.getSelectedHeroId());
                    data.put("skinId", p.getSkinId());
                    data.put("spell1", p.getSpell1() != null ? p.getSpell1() : "flash");
                    data.put("spell2", p.getSpell2() != null ? p.getSpell2() : "heal");
                    heroDataList.add(data);
                }
                fishBattleRoomService.updatePlayersHeroSelection(session.getDbRoomId(), heroDataList);
                log.info("英雄选择数据已更新: roomCode={}, dbRoomId={}", roomCode, session.getDbRoomId());
            } catch (Exception e) {
                log.error("英雄选择数据更新异常: roomCode={}", roomCode, e);
            }
        }

        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> completePayload = new LinkedHashMap<>();
        completePayload.put("roomCode", roomCode);
        completePayload.put("players", roomManager.getHeroPickSnapshot(roomCode));
        completePayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "game:heroPickComplete", completePayload);

        broadcastLobbyRoomUpdated(roomCode);

        log.info("选英雄阶段完成: roomCode={}", roomCode);
    }

    private void broadcastHeroPickUpdate(String roomCode) {
        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> updatePayload = new LinkedHashMap<>();
        updatePayload.put("players", roomManager.getHeroPickSnapshot(roomCode));
        updatePayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "hero:pickUpdate", updatePayload);
    }


    private void broadcastLoadingProgress(String roomCode) {
        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("players", roomManager.getRoomPlayersSnapshot(roomCode));
        payload.put("loadedCount", roomManager.getLoadedPlayerCount(roomCode));
        payload.put("totalPlayers", roomManager.getRoomPlayerCount(roomCode));
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "game:loadingProgressUpdate", payload);
    }

    /**
     * 广播最新玩家列表快照给房间所有人
     */
    private void broadcastPlayersSnapshot(String roomCode) {
        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> snapshotPayload = new LinkedHashMap<>();
        snapshotPayload.put("players", roomManager.getRoomPlayersSnapshot(roomCode));
        snapshotPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "room:playersUpdate", snapshotPayload);
    }

    /**
     * 广播玩家离开事件
     */
    private void broadcastPlayerLeft(FishBattleRoomManager.PlayerConnection conn) {
        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(conn.getRoomCode());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", conn.getUserId());
        payload.put("playerName", conn.getPlayerName());
        payload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "room:playerLeft", payload);
    }

    /**
     * 广播房主变更事件
     */
    private void broadcastOwnerChanged(String roomCode, Long newOwnerId, String newOwnerName) {
        Collection<SocketIOClient> clients = roomManager.getRoomClients(roomCode);
        Map<String, Object> ownerPayload = new LinkedHashMap<>();
        ownerPayload.put("newOwnerId", newOwnerId);
        ownerPayload.put("newOwnerName", newOwnerName != null ? newOwnerName : "未知玩家");
        ownerPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(clients, "room:ownerChanged", ownerPayload);
    }

    /**
     * 发送错误消息给客户端
     */
    private void sendError(SocketIOClient client, String msg) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", msg);
        err.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "room:error", err);
    }

    /**
     * 根据已存在房间的状态生成清晰的提示文案
     */
    private String buildExistingRoomMessage(String existingRoomCode) {
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(existingRoomCode);
        if (session == null) {
            return "你已在房间 " + existingRoomCode + " 中，请先离开当前房间";
        }
        switch (session.getStatus()) {
            case 0:
                return "你正在房间「" + session.getRoomName() + "」中等待，请先返回该房间或退出";
            case 1:
                return "你正在房间「" + session.getRoomName() + "」中选择英雄，请先完成当前对局";
            case 2:
                return "你正在房间「" + session.getRoomName() + "」中对局，请先完成当前对局";
            default:
                return "你已在房间 " + existingRoomCode + " 中，请先离开当前房间";
        }
    }

    /**
     * 发送错误消息给客户端，并附带已存在的房间信息（用于引导跳转）
     */
    private void sendErrorWithRoom(SocketIOClient client, String msg, String existingRoomCode) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", msg);
        err.put("existingRoomCode", existingRoomCode);
        // 携带房间状态，前端据此决定跳转目标页面
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(existingRoomCode);
        if (session != null) {
            err.put("existingRoomStatus", session.getStatus());
            err.put("existingRoomName", session.getRoomName());
        }
        err.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "room:error", err);
    }

    /**
     * 全员离线自动结束时，同步更新DB房间状态为已结束(status=3)
     */
    private void syncEndedRoomToDb(Long dbRoomId, String roomCode) {
        if (dbRoomId == null) {
            return;
        }
        try {
            fishBattleRoomService.updateRoomStatus(dbRoomId, 3);
            log.info("全员离线自动结束，DB房间状态已更新: dbRoomId={}, roomCode={}", dbRoomId, roomCode);
        } catch (Exception e) {
            log.error("全员离线自动结束，DB房间状态更新失败: dbRoomId={}, roomCode={}", dbRoomId, roomCode, e);
        }
    }

    /**
     * 查找指定队伍的第一个空位
     */
    private int findFirstEmptySlot(String roomCode, String team) {
        for (int i = 0; i < 5; i++) {
            if (!roomManager.isSlotOccupied(roomCode, team, i)) {
                return i;
            }
        }
        return 0;
    }

    private void handleHeroSelectSkin(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            sendError(client, "您不在任何房间中");
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || !session.isHeroPickPhase()) {
            sendError(client, "当前不在选英雄阶段");
            return;
        }

        String skinId = payload.path("skinId").asText(null);
        String skinSplashArt = payload.path("skinSplashArt").asText(null);
        String skinModelUrl = payload.path("skinModelUrl").asText(null);
        roomManager.selectSkin(roomCode, conn.getUserId(), skinId, skinSplashArt, skinModelUrl);
        broadcastHeroPickUpdate(roomCode);
    }

    private void handleHeroSelectSpells(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            sendError(client, "您不在任何房间中");
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || !session.isHeroPickPhase()) {
            sendError(client, "当前不在选英雄阶段");
            return;
        }

        String spell1 = payload.path("spell1").asText(null);
        String spell2 = payload.path("spell2").asText(null);
        roomManager.selectSpells(roomCode, conn.getUserId(), spell1, spell2);
        broadcastHeroPickUpdate(roomCode);
    }

    /**
     * 处理客户端进入3D对战页面后的 battle:join 事件。
     * 通过 battle3dRoomManager 加入战斗房间，返回权威初始英雄状态。
     */
    private void handleBattleJoin(SocketIOClient client, JsonNode payload) {
        Long userId = getAuthUserId(client);
        if (userId == null) {
            sendError(client, "请先登录");
            return;
        }
        String roomCode = payload != null ? payload.path("roomCode").asText(null) : null;
        if (roomCode == null) {
            sendError(client, "房间编码不能为空");
            return;
        }
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null) {
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("reason", "对局已结束或服务器已重启，房间不存在");
            broadcastService.sendToClient(client, "battle:roomNotFound", notFound);
            return;
        }
        if (session.getStatus() < 2) {
            sendError(client, "对局尚未开始");
            return;
        }

        // 如果战斗房间尚未创建（防御性：正常流程在 handleGameLoaded 中创建）
        if (!battle3dRoomManager.hasBattleRoom(roomCode)) {
            createBattleRoom(roomCode, session);
        }

        // 查找玩家名
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(client.getSessionId().toString());
        String playerName = conn != null ? conn.getPlayerName() : "玩家";

        // 加入战斗房间
        com.cong.fishisland.model.fishbattle.battle.PlayerSession battleSession =
                battle3dRoomManager.joinByUserId(roomCode, userId, playerName, client);

        // 构造初始英雄状态（从权威 BattleRoom 读取）
        com.cong.fishisland.model.fishbattle.battle.BattleRoom battleRoom = battle3dRoomManager.getRoom(roomCode);
        List<Map<String, Object>> champions = new ArrayList<>();
        if (battleRoom != null && battleRoom.getChampions() != null) {
            for (com.cong.fishisland.model.fishbattle.battle.BattleChampionState cs : battleRoom.getChampions()) {
                Map<String, Object> champ = new LinkedHashMap<>();
                champ.put("id", cs.getId());
                champ.put("heroId", cs.getHeroId());
                champ.put("playerName", cs.getPlayerName());
                champ.put("team", cs.getTeam());
                champ.put("heroModelUrl", cs.getModelUrl());
                champ.put("skin", cs.getSkin());
                champ.put("position", cs.getPosition());
                champ.put("rotation", cs.getRotation());
                champ.put("moveSpeed", cs.getMoveSpeed());
                champ.put("animationState", cs.getAnimationState());
                champ.put("hp", cs.getHp());
                champ.put("maxHp", cs.getMaxHp());
                champ.put("mp", cs.getMp());
                champ.put("maxMp", cs.getMaxMp());
                champions.add(champ);
            }
        }

        // 附加 userId → championId 映射
        Map<Long, String> userChampionMap = battle3dRoomManager.getUserChampionMapping(roomCode);

        Map<String, Object> battlePayload = new LinkedHashMap<>();
        battlePayload.put("roomCode", roomCode);
        battlePayload.put("myUserId", userId);
        battlePayload.put("myChampionId", battleSession != null ? battleSession.getChampionId() : null);
        battlePayload.put("champions", champions);
        battlePayload.put("userChampionMapping", userChampionMap);
        battlePayload.put("serverTime", System.currentTimeMillis());
        broadcastService.sendToClient(client, "battle:state", battlePayload);

        log.info("玩家加入对战场景: roomCode={}, userId={}, championId={}",
                roomCode, userId, battleSession != null ? battleSession.getChampionId() : "spectator");
    }

    /**
     * 处理客户端 3D 场景资产加载完毕的 battle:sceneReady 事件。
     * 当房间内所有在线玩家都完成场景加载后，广播 battle:allSceneReady。
     */
    private void handleBattleSceneReady(SocketIOClient client, JsonNode payload) {
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            return;
        }
        String roomCode = conn.getRoomCode();
        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session == null || session.getStatus() < 2) {
            return;
        }

        conn.setSceneReady(true);
        log.info("玩家场景就绪: roomCode={}, userId={}", roomCode, conn.getUserId());

        // 检查是否全员在线玩家都已场景就绪
        boolean allReady = session.getPlayers().values().stream()
                .filter(FishBattleRoomManager.PlayerConnection::isOnline)
                .allMatch(FishBattleRoomManager.PlayerConnection::isSceneReady);

        if (allReady) {
            log.info("全员场景就绪，广播 battle:allSceneReady: roomCode={}", roomCode);
            Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
            broadcastService.broadcast(roomClients, "battle:allSceneReady", new java.util.LinkedHashMap<>());
        }
    }

    /**
     * 处理 battle:ping，立即回复 battle:pong 供前端计算 RTT。
     */
    private void handleBattlePing(SocketIOClient client, JsonNode payload, com.corundumstudio.socketio.AckRequest ackRequest) {
        Map<String, Object> pongPayload = new LinkedHashMap<>();
        pongPayload.put("serverTime", System.currentTimeMillis());
        if (payload != null && payload.has("clientSendTime") && payload.path("clientSendTime").canConvertToLong()) {
            pongPayload.put("clientSendTime", payload.path("clientSendTime").asLong());
        }
        if (payload != null && payload.has("clientTime") && payload.path("clientTime").canConvertToLong()) {
            pongPayload.put("clientTime", payload.path("clientTime").asLong());
        }
        client.sendEvent("battle:pong", pongPayload);
        if (ackRequest != null && ackRequest.isAckRequested()) {
            ackRequest.sendAckData(pongPayload);
        }
    }

    private void handleGameLoadingProgress(SocketIOClient client, JsonNode payload) {
        if (payload == null) {
            return;
        }
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            return;
        }
        String roomCode = conn.getRoomCode();
        int progress = payload.path("progress").asInt(-1);
        if (progress < 0 || progress > 100) {
            sendError(client, "加载进度不合法");
            return;
        }
        boolean updated = roomManager.updateLoadingProgress(roomCode, conn.getUserId(), progress);
        if (!updated) {
            return;
        }
        broadcastLoadingProgress(roomCode);
    }

    private void handleGameLoaded(SocketIOClient client, JsonNode payload) {
        String sessionId = client.getSessionId().toString();
        FishBattleRoomManager.PlayerConnection conn = roomManager.getBySessionId(sessionId);
        if (conn == null) {
            return;
        }

        String roomCode = conn.getRoomCode();
        boolean marked = roomManager.markPlayerLoaded(roomCode, conn.getUserId());
        if (!marked) {
            return;
        }
        Collection<SocketIOClient> roomClients = roomManager.getRoomClients(roomCode);
        Map<String, Object> loadedPayload = new LinkedHashMap<>();
        loadedPayload.put("userId", conn.getUserId());
        loadedPayload.put("playerName", conn.getPlayerName());
        loadedPayload.put("loadedCount", roomManager.getLoadedPlayerCount(roomCode));
        loadedPayload.put("totalPlayers", roomManager.getRoomPlayerCount(roomCode));
        loadedPayload.put("serverTime", System.currentTimeMillis());
        broadcastService.broadcast(roomClients, "game:playerLoaded", loadedPayload);
        broadcastLoadingProgress(roomCode);

        FishBattleRoomManager.RoomSession session = roomManager.getRoomSession(roomCode);
        if (session != null && session.getStatus() == 1 && roomManager.areAllPlayersLoaded(roomCode)) {
            roomManager.enterPlaying(roomCode);
            if (session.getDbRoomId() != null) {
                try {
                    fishBattleRoomService.updateRoomStatus(session.getDbRoomId(), 2);
                    log.info("DB房间状态已更新为对局中: dbRoomId={}", session.getDbRoomId());
                } catch (Exception e) {
                    log.error("DB房间状态更新异常: dbRoomId={}", session.getDbRoomId(), e);
                }
            }

            // 全员加载完成后创建战斗房间
            createBattleRoom(roomCode, session);

            Map<String, Object> allLoadedPayload = new LinkedHashMap<>();
            allLoadedPayload.put("roomCode", roomCode);
            allLoadedPayload.put("serverTime", System.currentTimeMillis());
            broadcastService.broadcast(roomClients, "game:allPlayersLoaded", allLoadedPayload);
            broadcastLobbyRoomUpdated(roomCode);
            broadcastLobbyOverview();
            log.info("全员加载完成，进入对局状态: roomCode={}", roomCode);
        }
    }

    // ==================== 战斗房间创建 ====================

    /**
     * 创建权威战斗房间（由 Battle3dRoomManager 管理运行时状态）。
     * 在全员加载完成后调用，将选英雄阶段的玩家数据转换为战斗运行时状态。
     */
    private void createBattleRoom(String roomCode, FishBattleRoomManager.RoomSession session) {
        if (battle3dRoomManager.hasBattleRoom(roomCode)) {
            return;
        }
        try {
            battle3dRoomManager.createRoomFromPlayers(roomCode, session.getDbRoomId(), session.getPlayers().values());
            log.info("战斗房间已创建: roomCode={}", roomCode);
        } catch (Exception e) {
            log.error("战斗房间创建失败: roomCode={}", roomCode, e);
        }
    }

    // ==================== 对战移动/停止处理 ====================

    /**
     * 根据 SocketIOClient 查找对应的战斗会话。
     */
    private com.cong.fishisland.model.fishbattle.battle.PlayerSession findBattleSessionByClient(SocketIOClient client) {
        return battle3dRoomManager.findPlayerSessionBySessionId(client.getSessionId().toString());
    }

    /**
     * 处理 champion:move / move 事件。
     * 验证 championId 和会话，提取目标点和输入模式，入队到 InputQueue。
     */
    private void handleBattleMove(SocketIOClient client, JsonNode payload) {
        if (payload == null || payload.path("championId").isMissingNode()) {
            return;
        }
        String championId = payload.path("championId").asText();
        JsonNode pointNode = payload.has("targetPoint") && !payload.path("targetPoint").isNull()
                ? payload.path("targetPoint")
                : payload.has("target") && !payload.path("target").isNull()
                        ? payload.path("target") : null;
        if (pointNode == null || pointNode.isMissingNode() || pointNode.isNull()
                || !pointNode.has("x") || !pointNode.has("z")) {
            return;
        }
        com.cong.fishisland.model.fishbattle.battle.PlayerSession session = findBattleSessionByClient(client);
        if (session == null || !championId.equals(session.getChampionId())) {
            return;
        }
        com.cong.fishisland.model.fishbattle.battle.BattleRoom room =
                battle3dRoomManager.findRoomBySessionId(client.getSessionId().toString());
        if (room == null) {
            return;
        }
        String inputMode = payload.has("inputMode") ? payload.path("inputMode").asText("mouse") : "mouse";
        Long clientMoveSequence = payload.has("clientMoveSequence") && payload.path("clientMoveSequence").canConvertToLong()
                ? payload.path("clientMoveSequence").asLong()
                : null;
        Long clientTimestamp = payload.has("clientTimestamp") && payload.path("clientTimestamp").canConvertToLong()
                ? payload.path("clientTimestamp").asLong()
                : null;
        inputQueue.enqueue(com.cong.fishisland.model.fishbattle.battle.PlayerInput.builder()
                .type(com.cong.fishisland.model.fishbattle.battle.PlayerInput.Type.MOVE)
                .championId(championId)
                .roomId(room.getRoomId())
                .sessionId(client.getSessionId().toString())
                .clientSeq(clientMoveSequence)
                .clientTimestamp(clientTimestamp)
                .serverReceiveTime(System.currentTimeMillis())
                .targetX(pointNode.path("x").asDouble())
                .targetZ(pointNode.path("z").asDouble())
                .inputMode(inputMode)
                .rawPayload(payload)
                .build());
    }

    /**
     * 处理 champion:stop / stop 事件。
     * 验证 championId 和会话，入队到 InputQueue。
     */
    private void handleBattleStop(SocketIOClient client, JsonNode payload) {
        if (payload == null || payload.path("championId").isMissingNode()) {
            return;
        }
        String championId = payload.path("championId").asText();
        com.cong.fishisland.model.fishbattle.battle.PlayerSession session = findBattleSessionByClient(client);
        if (session == null || !championId.equals(session.getChampionId())) {
            return;
        }
        com.cong.fishisland.model.fishbattle.battle.BattleRoom room =
                battle3dRoomManager.findRoomBySessionId(client.getSessionId().toString());
        if (room == null) {
            return;
        }
        Long clientMoveSequence = payload.has("clientMoveSequence") && payload.path("clientMoveSequence").canConvertToLong()
                ? payload.path("clientMoveSequence").asLong()
                : null;
        Long clientTimestamp = payload.has("clientTimestamp") && payload.path("clientTimestamp").canConvertToLong()
                ? payload.path("clientTimestamp").asLong()
                : null;
        Double clientPositionX = payload.has("clientPosition") && payload.path("clientPosition").has("x")
                ? payload.path("clientPosition").path("x").asDouble()
                : null;
        Double clientPositionZ = payload.has("clientPosition") && payload.path("clientPosition").has("z")
                ? payload.path("clientPosition").path("z").asDouble()
                : null;
        inputQueue.enqueue(com.cong.fishisland.model.fishbattle.battle.PlayerInput.builder()
                .type(com.cong.fishisland.model.fishbattle.battle.PlayerInput.Type.STOP)
                .championId(championId)
                .roomId(room.getRoomId())
                .sessionId(client.getSessionId().toString())
                .clientSeq(clientMoveSequence)
                .clientTimestamp(clientTimestamp)
                .serverReceiveTime(System.currentTimeMillis())
                .clientPositionX(clientPositionX)
                .clientPositionZ(clientPositionZ)
                .rawPayload(payload)
                .build());
    }

    private void handleBattleCastSpell(SocketIOClient client, JsonNode payload) {
        enqueueBattleCastInput(client, payload, com.cong.fishisland.model.fishbattle.battle.PlayerInput.Type.CAST_SPELL);
    }

    private void handleBattleBasicAttack(SocketIOClient client, JsonNode payload) {
        enqueueBattleCastInput(client, payload, com.cong.fishisland.model.fishbattle.battle.PlayerInput.Type.BASIC_ATTACK);
    }

    private void enqueueBattleCastInput(SocketIOClient client, JsonNode payload,
                                        com.cong.fishisland.model.fishbattle.battle.PlayerInput.Type type) {
        if (payload == null || payload.path("casterId").isMissingNode()) {
            return;
        }
        String championId = payload.path("casterId").asText();
        com.cong.fishisland.model.fishbattle.battle.PlayerSession session = findBattleSessionByClient(client);
        if (session == null || !championId.equals(session.getChampionId())) {
            return;
        }
        com.cong.fishisland.model.fishbattle.battle.BattleRoom room =
                battle3dRoomManager.findRoomBySessionId(client.getSessionId().toString());
        if (room == null) {
            return;
        }
        Long clientMoveSequence = payload.has("clientMoveSequence") && payload.path("clientMoveSequence").canConvertToLong()
                ? payload.path("clientMoveSequence").asLong()
                : null;
        Long clientTimestamp = payload.has("clientTimestamp") && payload.path("clientTimestamp").canConvertToLong()
                ? payload.path("clientTimestamp").asLong()
                : null;
        Double clientPositionX = payload.has("clientPosition") && payload.path("clientPosition").has("x")
                ? payload.path("clientPosition").path("x").asDouble()
                : null;
        Double clientPositionZ = payload.has("clientPosition") && payload.path("clientPosition").has("z")
                ? payload.path("clientPosition").path("z").asDouble()
                : null;

        inputQueue.enqueue(com.cong.fishisland.model.fishbattle.battle.PlayerInput.builder()
                .type(type)
                .championId(championId)
                .roomId(room.getRoomId())
                .sessionId(client.getSessionId().toString())
                .clientSeq(clientMoveSequence)
                .clientTimestamp(clientTimestamp)
                .serverReceiveTime(System.currentTimeMillis())
                .requestId(payload.path("requestId").asText(null))
                .slot(payload.path("slot").asText(null))
                .skillId(payload.path("skillId").asText(null))
                .targetEntityId(payload.path("targetEntityId").asText(null))
                .targetPointX(payload.has("targetPoint") && payload.path("targetPoint").has("x") ? payload.path("targetPoint").path("x").asDouble() : null)
                .targetPointY(payload.has("targetPoint") && payload.path("targetPoint").has("y") ? payload.path("targetPoint").path("y").asDouble() : null)
                .targetPointZ(payload.has("targetPoint") && payload.path("targetPoint").has("z") ? payload.path("targetPoint").path("z").asDouble() : null)
                .targetDirectionX(payload.has("targetDirection") && payload.path("targetDirection").has("x") ? payload.path("targetDirection").path("x").asDouble() : null)
                .targetDirectionY(payload.has("targetDirection") && payload.path("targetDirection").has("y") ? payload.path("targetDirection").path("y").asDouble() : null)
                .targetDirectionZ(payload.has("targetDirection") && payload.path("targetDirection").has("z") ? payload.path("targetDirection").path("z").asDouble() : null)
                .clientPositionX(clientPositionX)
                .clientPositionZ(clientPositionZ)
                .rawPayload(payload)
                .build());
    }

    /**
     * 通用广播中继：将客户端发送的事件原样加上 serverTime 后广播给同房间所有玩家。
     * 用于 champion:animate / champion:emote / champion:voice 等无需服务端逻辑处理的交互事件。
     */
    private static final ObjectMapper RELAY_MAPPER = new ObjectMapper();

    private void handleBattleBroadcastRelay(SocketIOClient client, String eventType, JsonNode payload) {
        if (payload == null) { return; }
        com.cong.fishisland.model.fishbattle.battle.BattleRoom room =
                battle3dRoomManager.findRoomBySessionId(client.getSessionId().toString());
        if (room == null) { return; }
        Map<String, Object> relayPayload = new LinkedHashMap<>();
        relayPayload.put("serverTime", System.currentTimeMillis());
        // 将 JsonNode 转换为普通 Java 对象（Map/List/String/Number/Boolean），
        // 避免 netty-socketio 序列化 JsonNode 时产生非标准 JSON 结构。
        payload.fields().forEachRemaining(entry -> {
            try {
                relayPayload.put(entry.getKey(), RELAY_MAPPER.treeToValue(entry.getValue(), Object.class));
            } catch (Exception e) {
                log.warn("广播中继字段转换失败: key={}, eventType={}", entry.getKey(), eventType, e);
            }
        });
        battle3dBroadcastService.broadcast(room, eventType, relayPayload);
    }
}
