package com.cong.fishisland.game.landlords.ws;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cong.fishisland.game.enums.*;
import com.cong.fishisland.game.landlords.dto.request.PlayCardsReq;
import com.cong.fishisland.game.landlords.dto.request.RobLandlordReq;
import com.cong.fishisland.game.landlords.dto.response.GameStateResp;
import com.cong.fishisland.game.landlords.enums.GamePhaseEnum;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import com.cong.fishisland.game.landlords.service.GameBusinessException;
import com.cong.fishisland.game.landlords.service.LandlordsGameService;
import com.cong.fishisland.game.manager.GameRoomManager;
import com.cong.fishisland.game.manager.GameSessionManager;
import com.cong.fishisland.game.model.GameSession;
import com.cong.fishisland.game.model.dto.request.CreateRoomReq;
import com.cong.fishisland.game.model.dto.request.JoinRoomReq;
import com.cong.fishisland.game.model.dto.request.RoomListReq;
import com.cong.fishisland.game.model.dto.response.*;
import com.cong.fishisland.game.model.player.GamePlayer;
import com.cong.fishisland.game.model.room.GameRoom;
import com.cong.fishisland.game.ws.GameMessageHandler;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 斗地主游戏消息处理器
 * 统一管理游戏消息的处理和分发
 *
 * @author cong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LandlordsGameMessageHandler implements GameMessageHandler {

    private final GameRoomManager roomManager;
    private final GameSessionManager sessionManager;
    private final LandlordsGameService gameService;
    private final UserService userService;

    /**
     * 处理器映射表
     */
    private Map<GameMessageTypeEnum, BiFunction<String, Long, GameMessageResult>> handlers;

    @Override
    public GameTypeEnum getGameType() {
        return GameTypeEnum.LANDLORDS_CLASSIC;
    }

    @Override
    public Object handle(String messageType, String jsonContent, Long userId) {
        GameMessageTypeEnum type = GameMessageTypeEnum.of(messageType);
        if (type == null) {
            return GameMessageResult.error(messageType, "未知的消息类型");
        }

        BiFunction<String, Long, GameMessageResult> handler = handlers.get(type);
        if (handler == null) {
            return GameMessageResult.error(messageType, "该消息类型不支持");
        }

        try {
            return handler.apply(jsonContent, userId);
        } catch (GameBusinessException e) {
            log.warn("游戏业务异常: type={}, userId={}, error={}", type, userId, e.getMessage());
            return GameMessageResult.error(type.getType(), e.getMessage());
        } catch (Exception e) {
            log.error("处理游戏消息失败: type={}, userId={}", type, userId, e);
            return GameMessageResult.error(type.getType(), "服务器内部错误");
        }
    }

    @Override
    public void onDisconnect(Long userId) {
        String roomId = roomManager.getUserRoomId(userId);
        if (roomId != null) {
            GameRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                gameService.disconnect(room, userId);
                broadcastPlayerStatusChange(room, userId, PlayerStatusEnum.OFFLINE);

                // 检查是否所有玩家都离线了
                if (room.getOnlinePlayerCount() == 0) {
                    // 游戏中/叫地主阶段：强制结束游戏
                    if (room.getState() == RoomStateEnum.PLAYING || room.getState() == RoomStateEnum.ROBBING) {
                        gameService.forceEndGame(room, "所有玩家都已离线");
                    }
                    // 关闭房间
                    roomManager.removeRoom(roomId);
                }
            }
            roomManager.leaveRoom(roomId, userId);
        }
    }

    /**
     * 初始化处理器映射
     */
    @PostConstruct
    public void initHandlers() {
        handlers = new HashMap<>();

        // 房间管理
        handlers.put(GameMessageTypeEnum.CREATE_ROOM, this::handleCreateRoom);
        handlers.put(GameMessageTypeEnum.JOIN_ROOM, this::handleJoinRoom);
        handlers.put(GameMessageTypeEnum.LEAVE_ROOM, this::handleLeaveRoom);
        handlers.put(GameMessageTypeEnum.ROOM_LIST, this::handleRoomList);

        // 游戏准备
        handlers.put(GameMessageTypeEnum.READY, this::handleReady);
        handlers.put(GameMessageTypeEnum.START_GAME, this::handleStartGame);

        // 游戏进行
        handlers.put(GameMessageTypeEnum.ROB_LANDLORD, this::handleRobLandlord);
        handlers.put(GameMessageTypeEnum.PLAY_CARDS, this::handlePlayCards);
        handlers.put(GameMessageTypeEnum.PASS, this::handlePass);

        // AI托管
        handlers.put(GameMessageTypeEnum.CANCEL_ROBOT, this::handleCancelRobot);
        handlers.put(GameMessageTypeEnum.SET_ROBOT, this::handleSetRobot);

        // 其他
        handlers.put(GameMessageTypeEnum.CHAT, this::handleChat);
    }

    // ==================== 房间管理 ====================

    /**
     * 处理创建房间
     */
    private GameMessageResult handleCreateRoom(String json, Long userId) {
        CreateRoomReq req = parseJson(json, CreateRoomReq.class);

        GameTypeEnum gameType = req.getGameType() != null
                ? req.getGameType()
                : GameTypeEnum.LANDLORDS_CLASSIC;

        // 获取用户信息
        Map<String, String> userInfo = getUserInfo(userId);
        String userName = userInfo.get("userName");
        String userAvatar = userInfo.get("avatar");

        GameRoom room = roomManager.createRoom(gameType, userId, userName, userAvatar);


        return GameMessageResult.success(GameMessageTypeEnum.CREATE_ROOM.getType(),
                CreateRoomResp.builder()
                        .roomId(room.getRoomId())
                        .gameType(room.getGameType())
                        .roomInfo(room.toRoomInfoResp())
                        .build());
    }

    /**
     * 处理加入房间
     * 支持断线重连
     * 修改：加入前检查房间限制，但允许回到临时离开的房间
     */
    private GameMessageResult handleJoinRoom(String json, Long userId) {
        JoinRoomReq req = parseJson(json, JoinRoomReq.class);

        if (!StringUtils.hasText(req.getRoomId())) {
            return GameMessageResult.error(GameMessageTypeEnum.JOIN_ROOM.getType(), "房间号不能为空");
        }

        // 检查房间限制（但允许回到临时离开的房间）
        if (roomManager.hasRoomRestriction(userId)) {
            GameRoomManager.RoomRestrictionInfo restrictionInfo = roomManager.getRoomRestrictionInfo(userId);
            // 如果目标是临时离开的房间，允许加入（回到自己的房间）
            if (restrictionInfo != null && req.getRoomId().equals(restrictionInfo.getRoomId())) {
                // 允许回到自己的房间，不做限制
            } else {
                String message = String.format("你正在房间 %s 中游戏（%s），请先回到该房间或等待游戏结束后再加入其他房间",
                        restrictionInfo != null ? restrictionInfo.getRoomId() : "",
                        restrictionInfo != null ? restrictionInfo.getReason() : "");
                return GameMessageResult.error(GameMessageTypeEnum.JOIN_ROOM.getType(), message);
            }
        }

        // 获取用户信息
        Map<String, String> userInfo = getUserInfo(userId);
        String userName = userInfo.get("userName");
        String userAvatar = userInfo.get("avatar");

        // 检查是否是房间创建者（创建房间后加入不视为重连）
        GameRoom targetRoom = roomManager.getRoom(req.getRoomId());
        boolean isRoomOwner = targetRoom != null && targetRoom.getOwnerId() != null
                && targetRoom.getOwnerId().equals(userId);

        // 检查玩家是否已存在于房间中
        boolean isExistingPlayer = targetRoom != null && targetRoom.getPlayer(userId) != null;
        GameSession cachedSession = roomManager.getUserSession(userId);

        // 判断是否为真正的重连：玩家已在房间中且当前离线
        // 注意：创建房间后加入不视为重连，即使会话中记录了房间
        boolean isReconnecting = isExistingPlayer && cachedSession != null && !cachedSession.isOnline() && !isRoomOwner;

        GameRoom room = roomManager.joinRoom(req.getRoomId(), userId, userName, userAvatar, req.getPassword(), isRoomOwner);

        if (room == null) {
            if (isReconnecting) {
                return GameMessageResult.error(GameMessageTypeEnum.JOIN_ROOM.getType(), "重连失败，请重新加入");
            }
            return GameMessageResult.error(GameMessageTypeEnum.JOIN_ROOM.getType(), "加入房间失败");
        }

        // 清除临时离开状态（回到自己临时离开的房间时）
        GameSession session = roomManager.getUserSession(userId);
        if (session != null && session.hasTempLeave()
                && req.getRoomId().equals(session.getTempLeaveRoomId())) {
            session.clearTempLeave();
        }

        // 构建响应
        JoinRoomResp.JoinRoomRespBuilder responseBuilder = JoinRoomResp.builder()
                .roomId(room.getRoomId())
                .playerId(userId)
                .playerCount(room.getPlayerCount())
                .roomInfo(room.toRoomInfoResp())
                .reconnect(isReconnecting);

        // 总是返回游戏状态（包含房间信息和玩家列表）
        GameStateResp gameState = gameService.getGameState(room, userId);

        // 如果是重连，发送私人手牌（buildGameState 已统一排序，直接使用）
        if (isReconnecting) {
                    userId, room.getRoomId(), room.getState());

            // 取消AI托管
            GamePlayer reconnectPlayer = room.getPlayer(userId);
            if (reconnectPlayer != null && reconnectPlayer.isRobotControlled()) {
                reconnectPlayer.setOnline(true);
                gameService.cancelRobotControl(room, userId);
            }

            // 重连时广播完整的游戏状态给其他玩家（包含 players 列表，让前端更新所有人的在线状态）
            Map<String, Object> stateUpdateData = new HashMap<>();
            stateUpdateData.put("roomInfo", room.toRoomInfoResp());
            stateUpdateData.put("players", room.toRoomInfoResp().getPlayers());
            stateUpdateData.put("playerCount", room.getPlayerCount());
            stateUpdateData.put("phase", GamePhaseEnum.fromRoomState(room.getState()));
            stateUpdateData.put("roomState", room.getState());
            stateUpdateData.put("event", GameActionEnum.PLAYER_RECONNECT.getCode());
            stateUpdateData.put("reconnectUserId", userId);

                    userId, room.getPlayerOrder(), room.getPlayerCount(), userId);

            // 广播给除重连玩家外的所有玩家
            sessionManager.broadcastToRoomExcept(userId, room.getPlayerOrder(),
                    GameMessageTypeEnum.STATE_UPDATE.getType(), stateUpdateData);

            // 重连时也广播上线状态（确保前端收到）
            broadcastPlayerStatusChange(room, userId, PlayerStatusEnum.ONLINE);
        } else {
            // 新玩家加入，广播完整的游戏状态给其他所有玩家（包含 players 列表）
            Map<String, Object> stateUpdateData = new HashMap<>();
            stateUpdateData.put("roomInfo", room.toRoomInfoResp());
            stateUpdateData.put("players", room.toRoomInfoResp().getPlayers());
            stateUpdateData.put("playerCount", room.getPlayerCount());
            stateUpdateData.put("phase", GamePhaseEnum.fromRoomState(room.getState()));
            stateUpdateData.put("roomState", room.getState());
            stateUpdateData.put("event", GameActionEnum.PLAYER_JOIN.getCode());

            // 广播给除新玩家外的所有玩家
            sessionManager.broadcastToRoomExcept(userId, room.getPlayerOrder(),
                    GameMessageTypeEnum.STATE_UPDATE.getType(), stateUpdateData);
        }

        responseBuilder.gameState(gameState);

        return GameMessageResult.success(GameMessageTypeEnum.JOIN_ROOM.getType(), responseBuilder.build());
    }

    /**
     * 处理离开房间
     * 修改：游戏中离开时设置临时离开状态
     */
    private GameMessageResult handleLeaveRoom(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room != null) {
            GamePlayer player = room.getPlayer(userId);
            String playerName = player != null ? player.getUserName() : "玩家";

            RoomStateEnum state = room.getState();
            boolean inGame = state == RoomStateEnum.DISTRIBUTING || state == RoomStateEnum.ROBBING
                    || state == RoomStateEnum.PLAYING;
            boolean gameEnded = state == RoomStateEnum.ENDING || state == RoomStateEnum.CLOSED;

            if (inGame) {
                // 在游戏中离开，设置离线并进入AI托管
                if (player != null) {
                    player.setOnline(false);
                }
                gameService.setRobotControl(room, userId, RobotReasonEnum.LEAVE);
                broadcastPlayerStatusChange(room, userId, PlayerStatusEnum.OFFLINE);

                // 设置临时离开状态
                com.cong.fishisland.game.model.GameSession session = roomManager.getUserSession(userId);
                if (session != null) {
                    session.setTempLeave(room.getRoomId());
                    // 保存会话到 Redis
                    roomManager.saveSession(session);
                }


                // 检查是否所有玩家都离线了，如果是则强制结束游戏并删除房间
                if (room.getOnlinePlayerCount() == 0) {
                    log.info("所有玩家都已离线，强制结束游戏并关闭房间: roomId={}", room.getRoomId());
                    // 强制结束游戏
                    if (state == RoomStateEnum.PLAYING || state == RoomStateEnum.ROBBING) {
                        gameService.forceEndGame(room, "所有玩家都已离线");
                    }
                    // 关闭房间
                    roomManager.removeRoom(room.getRoomId());
                }

                // 返回临时离开信息
                java.util.Map<String, Object> resultData = new java.util.HashMap<>();
                resultData.put("tempLeaveRoomId", room.getRoomId());
                resultData.put("message", "游戏仍在进行中，你可以随时回来");
                return GameMessageResult.success(GameMessageTypeEnum.LEAVE_ROOM.getType(), resultData);
            } else if (gameEnded) {
                // 游戏已结束，玩家离开房间
                roomManager.leaveRoom(room.getRoomId(), userId);
                broadcastPlayerStatusChange(room, userId, PlayerStatusEnum.OFFLINE);

                if (room.getPlayerCount() > 0) {
                    RoomStateUpdateResp.RoomStateUpdateRespBuilder eventBuilder = RoomStateUpdateResp.builder()
                            .event(GameActionEnum.PLAYER_LEAVE.getCode())
                            .playerName(playerName)
                            .playerCount(room.getPlayerCount())
                            .roomInfo(room.toRoomInfoResp());
                    broadcastRoomEvent(room, userId, "playerLeave", eventBuilder.build());
                }
            } else {
                // 等待/准备状态，直接离开房间
                roomManager.leaveRoom(room.getRoomId(), userId);

                if (room.getPlayerCount() > 0) {
                    RoomStateUpdateResp.RoomStateUpdateRespBuilder eventBuilder = RoomStateUpdateResp.builder()
                            .event(GameActionEnum.PLAYER_LEAVE.getCode())
                            .playerName(playerName)
                            .playerCount(room.getPlayerCount())
                            .roomInfo(room.toRoomInfoResp());
                    broadcastRoomEvent(room, userId, "playerLeave", eventBuilder.build());
                }
            }
        }

        return GameMessageResult.success(GameMessageTypeEnum.LEAVE_ROOM.getType(), null);
    }

    /**
     * 处理房间列表
     * 修改：返回房间限制信息
     */
    private GameMessageResult handleRoomList(String json, Long userId) {
        RoomListReq req = parseJson(json, RoomListReq.class);

        GameTypeEnum gameType = req != null && req.getGameType() != null
                ? req.getGameType()
                : GameTypeEnum.LANDLORDS_CLASSIC;

        java.util.List<RoomInfoResp> roomInfoList = roomManager.getRoomList(gameType).stream()
                .map(info -> {
                    GameRoom room = roomManager.getRoom(info.getRoomId());
                    return room != null ? room.toRoomInfoResp() : null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 获取用户房间限制信息
        GameRoomManager.RoomRestrictionInfo restrictionInfo = roomManager.getRoomRestrictionInfo(userId);

        // 转换为 RoomListResp.RoomRestrictionInfo
        RoomListResp.RoomRestrictionInfo restriction = null;
        if (restrictionInfo != null) {
            restriction = RoomListResp.RoomRestrictionInfo.builder()
                    .roomId(restrictionInfo.getRoomId())
                    .gameType(restrictionInfo.getGameType())
                    .state(restrictionInfo.getState())
                    .reason(restrictionInfo.getReason())
                    .build();
        }

        return GameMessageResult.success(GameMessageTypeEnum.ROOM_LIST.getType(),
                RoomListResp.builder()
                        .rooms(roomInfoList)
                        .restriction(restriction)
                        .total(roomInfoList.size())
                        .build());
    }

    // ==================== 游戏准备 ====================

    /**
     * 处理准备
     */
    private GameMessageResult handleReady(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.READY.getType(), "你不在任何房间中");
        }

        GamePlayer player = room.getPlayer(userId);
        if (player == null) {
            return GameMessageResult.error(GameMessageTypeEnum.READY.getType(), "不在此房间中");
        }

        // 检查房间状态，只有 WAITING 状态才能准备
        if (room.getState() != RoomStateEnum.WAITING) {
            return GameMessageResult.error(GameMessageTypeEnum.READY.getType(),
                    "当前房间状态不允许准备（" + room.getState() + "）");
        }

        // 切换准备状态
        boolean newReadyState = !player.isReady();
        player.setReady(newReadyState);
        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

                userId, room.getRoomId(), newReadyState, room.isAllReady());

        // 广播完整的玩家列表（包含最新的 ready 状态）
        Map<String, Object> broadcastData = new HashMap<>();
        broadcastData.put("roomInfo", room.toRoomInfoResp());
        broadcastData.put("players", room.toRoomInfoResp().getPlayers());
        broadcastData.put("playerCount", room.getPlayerCount());
        sessionManager.broadcastToRoom(room.getPlayerOrder(),
                GameMessageTypeEnum.STATE_UPDATE.getType(), broadcastData);

        return GameMessageResult.success(GameMessageTypeEnum.READY.getType(), room.toRoomInfoResp());
    }

    /**
     * 处理开始游戏
     */
    private GameMessageResult handleStartGame(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.START_GAME.getType(), "你不在任何房间中");
        }

        // 检查是否是房主
        if (!room.getOwnerId().equals(userId)) {
            return GameMessageResult.error(GameMessageTypeEnum.START_GAME.getType(), "只有房主可以开始游戏");
        }

        // 检查玩家数量
        if (room.getPlayerCount() < 3) {
            return GameMessageResult.error(GameMessageTypeEnum.START_GAME.getType(), "斗地主需要至少3名玩家");
        }

        // 检查所有玩家是否都已准备
        if (!room.isAllReady()) {
            log.warn("开始游戏失败: 不是所有玩家都已准备, roomId={}", room.getRoomId());
            return GameMessageResult.error(GameMessageTypeEnum.START_GAME.getType(), "所有玩家必须准备才能开始游戏");
        }

        // 开始游戏（gameService.startGame 内部已统一排序手牌，直接使用）
        GameStateResp gameState = gameService.startGame(room);

        // 给请求者补充手牌（统一排序后设置）
        GamePlayer player = room.getPlayer(userId);
        if (player != null && player.getHand() != null && !player.getHand().isEmpty()) {
            List<Poker> sortedList = new ArrayList<>(player.getHand().getAll());
            sortedList.sort((a, b) -> {
                if (a.isUniversal() != b.isUniversal()) return a.isUniversal() ? 1 : -1;
                return b.getLandlordsSortValue() - a.getLandlordsSortValue();
            });
            gameState.setHandCards(GameStateResp.PokerCardVO.fromList(sortedList));
        }

        // 广播游戏开始（不含手牌）
        GameStateResp broadcastState = GameStateResp.builder()
                .roomId(room.getRoomId())
                .gameType(room.getGameType())
                .roomState(room.getState())
                .phase(GamePhaseEnum.ROBBING)
                .currentRobPlayerId(room.getCurrentRobPlayerId())
                .highestRobScore(room.getHighestRobScore())
                .players(gameState.getPlayers())
                .build();

        sessionManager.broadcastToRoom(room.getPlayerOrder(),
                GameMessageTypeEnum.START_GAME.getType(), broadcastState);

        return GameMessageResult.success(GameMessageTypeEnum.START_GAME.getType(), gameState);
    }

    // ==================== 游戏进行 ====================

    /**
     * 处理叫地主
     */
    private GameMessageResult handleRobLandlord(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.ROB_LANDLORD.getType(), "你不在任何房间中");
        }

        if (room.getState() != RoomStateEnum.ROBBING) {
            return GameMessageResult.error(GameMessageTypeEnum.ROB_LANDLORD.getType(), "当前不在叫地主阶段");
        }

        if (!room.getCurrentRobPlayerId().equals(userId)) {
            return GameMessageResult.error(GameMessageTypeEnum.ROB_LANDLORD.getType(), "还没轮到你叫地主");
        }

        RobLandlordReq req = parseJson(json, RobLandlordReq.class);
        GameStateResp gameState = gameService.robLandlord(room, userId, req.getAction());

        return GameMessageResult.success(GameMessageTypeEnum.ROB_LANDLORD.getType(), gameState);
    }

    /**
     * 处理出牌
     */
    private GameMessageResult handlePlayCards(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.PLAY_CARDS.getType(), "你不在任何房间中");
        }

        if (room.getState() != RoomStateEnum.PLAYING) {
            return GameMessageResult.error(GameMessageTypeEnum.PLAY_CARDS.getType(), "当前不在出牌阶段");
        }

        if (!room.getCurrentPlayerId().equals(userId)) {
            return GameMessageResult.error(GameMessageTypeEnum.PLAY_CARDS.getType(), "还没轮到你出牌");
        }

        PlayCardsReq req = parseJson(json, PlayCardsReq.class);
        if (req.getPokers() == null || req.getPokers().isEmpty()) {
            return GameMessageResult.error(GameMessageTypeEnum.PLAY_CARDS.getType(), "请选择要出的牌");
        }

        GameStateResp gameState = gameService.playCards(room, userId, req.getPokers());

        return GameMessageResult.success(GameMessageTypeEnum.PLAY_CARDS.getType(), gameState);
    }

    /**
     * 处理不出
     */
    private GameMessageResult handlePass(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.PASS.getType(), "你不在任何房间中");
        }

        if (room.getState() != RoomStateEnum.PLAYING) {
            return GameMessageResult.error(GameMessageTypeEnum.PASS.getType(), "当前不在出牌阶段");
        }

        if (!room.getCurrentPlayerId().equals(userId)) {
            return GameMessageResult.error(GameMessageTypeEnum.PASS.getType(), "还没轮到你出牌");
        }

        GameStateResp gameState = gameService.pass(room, userId);

        return GameMessageResult.success(GameMessageTypeEnum.PASS.getType(), gameState);
    }

    // ==================== AI托管 ====================

    /**
     * 处理取消AI托管
     */
    private GameMessageResult handleCancelRobot(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.CANCEL_ROBOT.getType(), "你不在任何房间中");
        }

        GamePlayer player = room.getPlayer(userId);
        if (player == null) {
            return GameMessageResult.error(GameMessageTypeEnum.CANCEL_ROBOT.getType(), "不在此房间中");
        }

        if (!player.isRobotControlled()) {
            return GameMessageResult.error(GameMessageTypeEnum.CANCEL_ROBOT.getType(), "当前不在AI托管状态");
        }

        gameService.cancelRobotControl(room, userId);


        return GameMessageResult.success(GameMessageTypeEnum.CANCEL_ROBOT.getType(), null);
    }

    /**
     * 处理设置AI托管（主动托管）
     */
    private GameMessageResult handleSetRobot(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.SET_ROBOT.getType(), "你不在任何房间中");
        }

        GamePlayer player = room.getPlayer(userId);
        if (player == null) {
            return GameMessageResult.error(GameMessageTypeEnum.SET_ROBOT.getType(), "不在此房间中");
        }

        if (player.isRobotControlled()) {
            return GameMessageResult.error(GameMessageTypeEnum.SET_ROBOT.getType(), "已经在AI托管状态");
        }

        // 检查是否在游戏中
        if (!room.getState().isPlaying()) {
            return GameMessageResult.error(GameMessageTypeEnum.SET_ROBOT.getType(), "游戏未开始，无法托管");
        }

        gameService.setRobotControl(room, userId, RobotReasonEnum.MANUAL);


        return GameMessageResult.success(GameMessageTypeEnum.SET_ROBOT.getType(), null);
    }

    // ==================== 其他 ====================

    /**
     * 处理聊天
     */
    private GameMessageResult handleChat(String json, Long userId) {
        GameRoom room = getUserRoom(userId);
        if (room == null) {
            return GameMessageResult.error(GameMessageTypeEnum.CHAT.getType(), "你不在任何房间中");
        }

        try {
            JSONObject jsonObj = JSONUtil.parseObj(json);
            String content = jsonObj.getStr("content");
            String userName = jsonObj.getStr("userName");

            if (!StringUtils.hasText(content)) {
                return GameMessageResult.error(GameMessageTypeEnum.CHAT.getType(), "消息内容不能为空");
            }

            if (content.length() > 200) {
                return GameMessageResult.error(GameMessageTypeEnum.CHAT.getType(), "消息内容不能超过200字");
            }

            // 广播聊天消息
            Map<String, Object> chatData = new HashMap<>();
            chatData.put("userId", userId);
            chatData.put("userName", userName != null ? userName : "玩家");
            chatData.put("content", content);

            sessionManager.broadcastToRoom(room.getPlayerOrder(),
                    GameMessageTypeEnum.CHAT.getType(), chatData);

            return GameMessageResult.success(GameMessageTypeEnum.CHAT.getType(), null);
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            return GameMessageResult.error(GameMessageTypeEnum.CHAT.getType(), "发送消息失败");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析 JSON
     */
    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        }
        try {
            return JSONUtil.toBean(json, clazz);
        } catch (Exception e) {
            log.error("parseJson 失败, class={}, json={}", clazz.getName(), json, e);
            return null;
        }
    }

    /**
     * 获取用户当前所在房间
     */
    private GameRoom getUserRoom(Long userId) {
        String roomId = roomManager.getUserRoomId(userId);
        return roomId != null ? roomManager.getRoom(roomId) : null;
    }

    /**
     * 广播房间事件
     */
    private void broadcastRoomEvent(GameRoom room, Long excludeUserId, String event, Object data) {
        sessionManager.broadcastToRoomExcept(excludeUserId, room.getPlayerOrder(),
                GameMessageTypeEnum.STATE_UPDATE.getType(), data);
    }

    /**
     * 广播玩家状态变化（在线/离线）
     */
    private void broadcastPlayerStatusChange(GameRoom room, Long userId, PlayerStatusEnum status) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("status", status.getCode());
        data.put("event", GameActionEnum.PLAYER_STATUS_CHANGE.getCode());

        sessionManager.broadcastToRoom(room.getPlayerOrder(),
                GameMessageTypeEnum.STATE_UPDATE.getType(), data);
    }

    /**
     * 获取用户信息
     */
    private Map<String, String> getUserInfo(Long userId) {
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("userName", "未知玩家");
        userInfo.put("avatar", "");

        try {
            User user = userService.getById(userId);
            if (user != null) {
                userInfo.put("userName", user.getUserName());
                userInfo.put("avatar", user.getUserAvatar() != null ? user.getUserAvatar() : "");
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: userId={}", userId, e);
        }

        return userInfo;
    }
}
