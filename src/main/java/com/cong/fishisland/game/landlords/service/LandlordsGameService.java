package com.cong.fishisland.game.landlords.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.cong.fishisland.game.constant.GameConstants;
import com.cong.fishisland.game.enums.*;
import com.cong.fishisland.game.landlords.dto.response.ActionResultResp;
import com.cong.fishisland.game.landlords.dto.response.GameStateResp;
import com.cong.fishisland.game.landlords.dto.response.TurnNotifyResp;
import com.cong.fishisland.game.landlords.enums.GamePhaseEnum;
import com.cong.fishisland.game.landlords.enums.poker.PokerPatternEnum;
import com.cong.fishisland.game.landlords.model.poker.PatternResult;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import com.cong.fishisland.game.landlords.model.poker.PokerHand;
import com.cong.fishisland.game.landlords.util.poker.PokerComparator;
import com.cong.fishisland.game.landlords.util.poker.PokerGenerator;
import com.cong.fishisland.game.landlords.util.poker.PokerPatternMatcher;
import com.cong.fishisland.game.landlords.util.poker.PokerSorter;
import com.cong.fishisland.game.cache.GameRoomRedisCache;
import com.cong.fishisland.game.manager.GameRoomManager;
import com.cong.fishisland.game.manager.GameSessionManager;
import com.cong.fishisland.game.model.player.GamePlayer;
import com.cong.fishisland.game.model.room.GameRoom;
import com.cong.fishisland.game.service.GameService;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 斗地主游戏服务
 * 使用枚举和实体类统一管理游戏流程
 *
 * @author cong
 */
@Slf4j
@Service
public class LandlordsGameService implements GameService {

    @Resource
    private GameSessionManager sessionManager;

    @Resource
    private GameRoomManager roomManager;

    @Resource
    private GameRoomRedisCache roomCache;

    @Resource
    private LandlordsRobotService robotService;

    private final Map<String, ScheduledFuture<?>> robTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> playTimeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // ==================== 服务接口实现 ====================

    @Override
    public GameTypeEnum getGameType() {
        return GameTypeEnum.LANDLORDS_CLASSIC;
    }

    @Override
    public GameStateResp startGame(GameRoom room) {
        validatePlayerCount(room);

        // 更新房间状态
        room.setState(RoomStateEnum.DISTRIBUTING);

        // 生成并洗牌
        PokerHand deck = PokerGenerator.shuffle(PokerGenerator.generateFullDeck());

        // 发牌
        PokerGenerator.DealResult dealResult = PokerGenerator.dealWithBottom(
                deck, room.getPlayerCount(), GameConstants.INITIAL_HAND_CARDS, GameConstants.BOTTOM_CARD_COUNT);

        room.setBottomCards(dealResult.getBottom());

        // 分配手牌给玩家
        List<GamePlayer> players = room.getOrderedPlayers();
        for (int i = 0; i < players.size(); i++) {
            PokerHand hand = dealResult.getHands().get(i);
            PokerSorter.sortByLandlordsWithUniversal(hand);
            players.get(i).setHand(hand);
        }

        // 设置叫地主阶段
        int randomIndex = new Random().nextInt(players.size());
        Long firstRobPlayerId = players.get(randomIndex).getUserId();
        room.setCurrentRobPlayerId(firstRobPlayerId);
        room.setHighestRobScore(0);
        room.setRobbedPlayers(new HashSet<>());
        room.setPassedRobPlayers(new HashSet<>());
        room.setRobRoundStartPlayerId(firstRobPlayerId);
        room.setLastRobPlayerId(null);
        room.setState(RoomStateEnum.ROBBING);
        room.updateLastActiveTime();

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

        // 发送私人手牌给每个玩家
        for (GamePlayer player : players) {
            sendPrivateState(room, player, player.getUserId());
        }

        // 广播游戏开始 + 第一个叫地主回合通知
        broadcastGameStart(room);
        broadcastRobTurnNotify(room, firstRobPlayerId);

        // 启动叫地主超时
        startRobTimeout(room);

        return buildGameState(room, null);
    }

    @Override
    public GameStateResp robLandlord(GameRoom room, Long userId, Integer action) {
        // 验证当前状态
        GameValidationResult validation = validateRobAction(room, userId, action);
        if (!validation.isValid()) {
            throw new GameBusinessException(validation.getErrorCode(), validation.getErrorMessage());
        }

        // 取消超时
        cancelRobTimeout(room.getRoomId());

        // 获取玩家并记录叫分
        GamePlayer player = room.getPlayer(userId);
        player.setRobScore(action);

        // 更新最高分
        if (action > room.getHighestRobScore()) {
            room.setHighestRobScore(action);
        }

        // 构建叫分结果（在更新最高分之后）
        String robScoreDesc = action > 0 ? action + "分" : "不叫";
        String message = action > 0
                ? String.format("叫了 %d 分", action)
                : "不叫";

        // 广播叫分结果（统一格式）
        ActionResultResp actionResult = ActionResultResp.robResult(
                userId,
                player.getUserName(),
                action,
                robScoreDesc,
                room.getHighestRobScore(),
                message
        );
        broadcastActionResult(room, actionResult);

        // 处理叫地主完成
        GameStateResp result = handleRobCompletion(room, userId, action);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

        return result;
    }

    @Override
    public GameStateResp playCards(GameRoom room, Long userId, List<String> pokerIds) {

        // 验证出牌
        GameValidationResult validation = validatePlayCards(room, userId, pokerIds);
        if (!validation.isValid()) {
            throw new GameBusinessException(validation.getErrorCode(), validation.getErrorMessage());
        }

        // 取消超时
        cancelPlayTimeout(room.getRoomId());

        // 解析并移除手牌
        GamePlayer player = room.getPlayer(userId);
        List<Poker> playedPokers = parsePokers(pokerIds);
        // 按面值移除（与 containsCardByValue 校验一致），避免因花色或癞子状态导致 Poker.equals 失败而漏移除
        playedPokers.forEach(p -> removeCardByValue(player.getHand(), p));

        // 设置玩家的当前出牌
        player.setCurrentPlayedCards(playedPokers);

        // 分析牌型
        PokerHand playedHand = new PokerHand(playedPokers);
        PatternResult pattern = PokerPatternMatcher.analyze(playedHand);

        // 检查是否炸弹
        boolean isBomb = PokerPatternEnum.JOKER_BOMB.equals(pattern.getPattern())
                || PokerPatternEnum.BOMB.equals(pattern.getPattern());
        String patternDesc = PokerComparator.getPatternDescription(pattern);

        // 更新房间状态
        room.setLastPlayedCards(playedHand);
        room.setLastPlayerId(userId);

        // 广播出牌结果（统一格式）
        ActionResultResp actionResult = ActionResultResp.playResult(
                userId,
                player.getUserName(),
                playedPokers,
                patternDesc,
                isBomb,
                patternDesc
        );
        broadcastActionResult(room, actionResult);

        // 检查是否游戏结束
        if (player.getHand().isEmpty()) {
            return handleGameOver(room, userId);
        }

        // 发送私人状态
        sendPrivateState(room, player, userId);

        // 切换到下一个玩家
        Long nextPlayerId = room.getNextPlayerId(userId);
        room.setCurrentPlayerId(nextPlayerId);

        // 广播游戏状态（让所有玩家看到当前出的牌）
        broadcastGameState(room, null);

        // 判断下一个玩家是否可以不出（需要检查上家是否出过牌）
        boolean canPass = room.getLastPlayerId() != null && !room.getLastPlayerId().equals(nextPlayerId);

        // 广播下一个出牌回合通知（内部已处理超时和托管逻辑）
        broadcastPlayTurnNotify(room, nextPlayerId, canPass);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

        return buildGameState(room, null);
    }

    @Override
    public GameStateResp pass(GameRoom room, Long userId) {

        // 验证是否轮到自己
        if (!room.getCurrentPlayerId().equals(userId)) {
            throw new GameBusinessException("NOT_YOUR_TURN", "还没轮到你出牌");
        }

        // 验证是否可以不出
        GameValidationResult validation = validateCanPass(room, userId);
        if (!validation.isValid()) {
            throw new GameBusinessException(validation.getErrorCode(), validation.getErrorMessage());
        }

        // 取消超时
        cancelPlayTimeout(room.getRoomId());

        // 获取玩家并清空其上一轮留在桌面的牌
        GamePlayer player = room.getPlayer(userId);
        player.clearCurrentPlayedCards();
        String message = "不出";

        // 广播不出结果（统一格式）
        ActionResultResp actionResult = ActionResultResp.passResult(userId, player.getUserName(), message);
        broadcastActionResult(room, actionResult);

        // 检查是否需要重置出牌区（一轮结束）
        if (room.getNextPlayerId(userId).equals(room.getLastPlayerId())) {
            room.setLastPlayedCards(new PokerHand());
            room.setLastPlayerId(null);
            // 清除所有玩家的当前出牌
            room.getOrderedPlayers().forEach(p -> p.clearCurrentPlayedCards());
        }
        // 如果不是一轮结束，不清除，下次出牌直接覆盖

        // 切换到下一个玩家
        Long nextPlayerId = room.getNextPlayerId(userId);
        room.setCurrentPlayerId(nextPlayerId);

        // 广播游戏状态
        broadcastGameState(room, null);

        // 判断下一个玩家是否可以不出
        boolean canPass = room.getLastPlayerId() != null && !room.getLastPlayerId().equals(nextPlayerId);

        // 广播下一个出牌回合通知（内部已处理超时和托管逻辑）
        broadcastPlayTurnNotify(room, nextPlayerId, canPass);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

        return buildGameState(room, null);
    }

    @Override
    public GameStateResp getGameState(GameRoom room) {
        return buildGameState(room, null);
    }

    @Override
    public GameStateResp getGameState(GameRoom room, Long viewerId) {
        return buildGameState(room, viewerId);
    }

    @Override
    public void reconnect(GameRoom room, Long userId) {
        GamePlayer player = room.getPlayer(userId);
        if (player != null) {
            player.setOnline(true);
            sendPrivateState(room, player, userId);
            // 同步房间状态到 Redis
            roomManager.saveRoom(room);
        }
    }

    @Override
    public void disconnect(GameRoom room, Long userId) {
        GamePlayer player = room.getPlayer(userId);
        if (player != null) {
            player.setOnline(false);
            // 同步房间状态到 Redis
            roomManager.saveRoom(room);
        }
    }

    // ==================== 验证方法 ====================

    /**
     * 验证叫地主动作
     */
    private GameValidationResult validateRobAction(GameRoom room, Long userId, Integer action) {
        // 检查阶段
        if (room.getState() != RoomStateEnum.ROBBING) {
            return GameValidationResult.invalid("PHASE_MISMATCH", "当前不在叫地主阶段");
        }

        // 检查是否是当前玩家
        if (!room.getCurrentRobPlayerId().equals(userId)) {
            return GameValidationResult.invalid("NOT_YOUR_TURN", "还没轮到你叫地主");
        }

        // 检查叫分有效性
        if (action < 0 || action > 3) {
            return GameValidationResult.invalid("INVALID_ACTION", "无效的叫分");
        }

        // 检查是否超过最高分（必须严格大于当前最高分，不允许叫相同分数）
        if (action > 0 && action <= room.getHighestRobScore()) {
            return GameValidationResult.invalid("INVALID_ACTION", "叫分必须高于当前最高分");
        }

        return GameValidationResult.valid();
    }

    /**
     * 验证出牌
     */
    private GameValidationResult validatePlayCards(GameRoom room, Long userId, List<String> pokerIds) {
        // 检查阶段
        if (room.getState() != RoomStateEnum.PLAYING) {
            return GameValidationResult.invalid("PHASE_MISMATCH", "当前不在出牌阶段");
        }

        // 检查是否是当前玩家
        if (!room.getCurrentPlayerId().equals(userId)) {
            return GameValidationResult.invalid("NOT_YOUR_TURN", "还没轮到你出牌");
        }

        // 检查牌是否为空
        if (pokerIds == null || pokerIds.isEmpty()) {
            return GameValidationResult.invalid("INVALID_CARDS", "请选择要出的牌");
        }

        // 解析牌
        GamePlayer player = room.getPlayer(userId);
        List<Poker> playedPokers = parsePokers(pokerIds);

        // 检查牌是否在手牌中（按面值比较，因为解析时花色可能不一致）
        for (Poker playedPoker : playedPokers) {
            if (!containsCardByValue(player.getHand(), playedPoker)) {
                return GameValidationResult.invalid("INVALID_CARDS", "手牌中没有这些牌");
            }
        }

        // 检查牌型是否合法
        PokerHand playedHand = new PokerHand(playedPokers);
        PokerHand lastPlayedCards = room.getLastPlayedCards();
        boolean isFirstPlay = room.getLastPlayerId() == null || room.getLastPlayerId().equals(userId);

        if (!PokerPatternMatcher.isValidPlay(playedPokers,
                lastPlayedCards != null && !lastPlayedCards.isEmpty()
                        ? PokerPatternMatcher.analyze(lastPlayedCards) : null,
                isFirstPlay)) {
            return GameValidationResult.invalid("INVALID_CARDS", "出牌不合法");
        }

        return GameValidationResult.valid();
    }

    /**
     * 验证是否可以不出
     */
    private GameValidationResult validateCanPass(GameRoom room, Long userId) {
        Long lastPlayerId = room.getLastPlayerId();
        PokerHand lastPlayedCards = room.getLastPlayedCards();

        // 如果是第一个出牌，不能不出
        if (lastPlayerId == null || lastPlayerId.equals(userId)) {
            return GameValidationResult.invalid("CANNOT_PASS", "第一个出牌不能选择不出");
        }

        // 如果上家还没出牌，不能不出
        if (lastPlayedCards == null || lastPlayedCards.isEmpty()) {
            return GameValidationResult.invalid("CANNOT_PASS", "上家还没出牌，你不能跳过");
        }

        return GameValidationResult.valid();
    }

    // ==================== 业务处理方法 ====================

    /**
     * 处理叫地主完成
     */
    private GameStateResp handleRobCompletion(GameRoom room, Long currentUserId, int action) {
        // 如果叫了3分，直接确定地主（延迟3秒展示后再进入出牌）
        if (action == 3) {
            return handleRobWith3Points(room, currentUserId);
        }

        // 记录最后一个叫分的玩家
        if (action > 0) {
            room.setLastRobPlayerId(currentUserId);
            room.getRobbedPlayers().add(currentUserId);
        } else {
            room.getPassedRobPlayers().add(currentUserId);
        }

        // 判断一轮是否结束：轮到轮次开始玩家的下一个玩家时，一轮结束
        Long nextPlayerId = room.getNextPlayerId(currentUserId);
        boolean roundEnded = nextPlayerId.equals(room.getRobRoundStartPlayerId());

        if (roundEnded) {
            // 一轮结束，检查结果
            // 如果没人叫过分，重新发牌
            if (room.getRobbedPlayers().isEmpty()) {
                // 重置叫分状态
                room.setRobRoundStartPlayerId(nextPlayerId);
                room.setLastRobPlayerId(null);
                room.getPassedRobPlayers().clear();
                // 重新开始
                return startGame(room);
            }

            // 最后一个叫分的玩家是地主
            Long landlordId = room.getLastRobPlayerId();
            return determineLandlord(room, landlordId);
        }

        // 一轮未结束，切换到下一个叫地主的玩家
        room.setCurrentRobPlayerId(nextPlayerId);

        // 广播下一个叫地主回合通知
        broadcastRobTurnNotify(room, nextPlayerId);

        // 启动叫地主超时
        startRobTimeout(room);

        return buildGameState(room, null);
    }

    /**
     * 处理叫了3分的情况 - 延迟3秒展示后再确定地主
     */
    private GameStateResp handleRobWith3Points(GameRoom room, Long currentUserId) {

        // 取消现有的超时
        cancelRobTimeout(room.getRoomId());

        // 广播叫分结果（展示3秒）
        String robScoreDesc = "3分";
        GamePlayer player = room.getPlayer(currentUserId);
        String message = "叫了 3 分";

        ActionResultResp actionResult = ActionResultResp.robResult(
                currentUserId,
                player.getUserName(),
                3,
                robScoreDesc,
                3,
                message
        );
        broadcastActionResult(room, actionResult);

        // 延迟3秒后确定地主并进入出牌阶段
        final Long landlordId = currentUserId;
        scheduler.schedule(() -> {
            // 直接调用内部确定地主逻辑（不重新走 handleRobCompletion）
            determineLandlordInternal(room, landlordId);
        }, 3, TimeUnit.SECONDS);

        return buildGameState(room, null);
    }

    /**
     * 内部方法：确定地主（不通过 handleRobCompletion，避免递归）
     */
    private void determineLandlordInternal(GameRoom room, Long landlordId) {

        // 设置地主
        room.setLandlord(landlordId);
        room.setState(RoomStateEnum.PLAYING);
        room.setCurrentPlayerId(landlordId);
        room.setLastPlayerId(landlordId);
        room.setLastPlayedCards(new PokerHand());

        // 取消叫地主超时
        cancelRobTimeout(room.getRoomId());

        // 给每个玩家发送私人状态（包含底牌信息）
        for (GamePlayer player : room.getOrderedPlayers()) {
            sendPrivateState(room, player, player.getUserId());
        }

        // 广播地主确定（统一格式）
        ActionResultResp actionResult = ActionResultResp.landlordConfirmed(
                landlordId,
                room.getPlayer(landlordId).getUserName(),
                room.getBottomCards().getAll(),
                "成为地主"
        );
        broadcastActionResult(room, actionResult);

        // 广播出牌回合通知（内部已处理超时逻辑）
        broadcastPlayTurnNotify(room, landlordId, false);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);
    }

    /**
     * 确定地主
     */
    private GameStateResp determineLandlord(GameRoom room, Long landlordId) {

        // 设置地主
        room.setLandlord(landlordId);
        room.setState(RoomStateEnum.PLAYING);
        room.setCurrentPlayerId(landlordId);
        room.setLastPlayerId(landlordId);
        room.setLastPlayedCards(new PokerHand());

        // 取消叫地主超时
        cancelRobTimeout(room.getRoomId());

        // 给每个玩家发送私人状态（包含底牌信息）
        for (GamePlayer player : room.getOrderedPlayers()) {
            sendPrivateState(room, player, player.getUserId());
        }

        // 广播地主确定（统一格式）
        ActionResultResp actionResult = ActionResultResp.landlordConfirmed(
                landlordId,
                room.getPlayer(landlordId).getUserName(),
                room.getBottomCards().getAll(),
                "成为地主"
        );
        broadcastActionResult(room, actionResult);

        // 广播出牌回合通知（内部已处理超时逻辑）
        broadcastPlayTurnNotify(room, landlordId, false);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

        return buildGameState(room, landlordId);
    }

    /**
     * 处理游戏结束
     */
    private GameStateResp handleGameOver(GameRoom room, Long winnerId) {
        log.info("游戏结束: roomId={}, winnerId={}", room.getRoomId(), winnerId);

        // 保持 ENDING 状态，让前端显示结果
        room.setState(RoomStateEnum.ENDING);
        cancelRobTimeout(room.getRoomId());
        cancelPlayTimeout(room.getRoomId());

        // 清理离线玩家的用户-房间映射（允许他们加入其他房间）
        for (GamePlayer player : room.getOrderedPlayers()) {
            if (!player.isOnline()) {
                roomCache.removeUserRoom(player.getUserId());
            }
        }

        GamePlayer winner = room.getPlayer(winnerId);

        // 赢家手牌已出完，保持为空
        // 其他玩家的手牌和出牌区域保留不动

        boolean isLandlordWin = room.isLandlord(winnerId);
        String winTeam = isLandlordWin ? "地主" : "农民";

        // 构建玩家结果
        List<ActionResultResp.PlayerResultVO> playerResults = room.getOrderedPlayers().stream()
                .map(p -> ActionResultResp.PlayerResultVO.builder()
                        .userId(p.getUserId())
                        .userName(p.getUserName())
                        .isWinner(p.getUserId().equals(winnerId))
                        .isLandlord(p.isLandlord())
                        .build())
                .collect(Collectors.toList());

        String message = String.format("游戏结束！%s 获胜！(%s方获胜)", winner.getUserName(), winTeam);

        // 广播游戏结束（统一格式）
        ActionResultResp actionResult = ActionResultResp.gameOver(
                winnerId,
                winner.getUserName(),
                isLandlordWin,
                winTeam,
                playerResults,
                message
        );
        broadcastActionResult(room, actionResult);

        // 广播状态更新（保留所有玩家的手牌和出牌区域）
        GameStateResp gameState = buildGameState(room, null);
        broadcastGameState(room, null);

        // 重置房间状态为等待状态，允许玩家重新准备开始下一局
        resetRoomForNewRound(room);

        // 广播房间重置（房间回到等待状态，玩家需要重新准备）
        // 广播 roomInfo 让前端更新房间状态
        Map<String, Object> resetData = new HashMap<>();
        resetData.put("roomInfo", room.toRoomInfoResp());
        resetData.put("players", room.toRoomInfoResp().getPlayers());
        resetData.put("playerCount", room.getPlayerCount());
        resetData.put("roomState", RoomStateEnum.WAITING);
        resetData.put("phase", GamePhaseEnum.WAITING);
        sessionManager.broadcastToRoom(room.getPlayerOrder(),
                GameMessageTypeEnum.STATE_UPDATE.getType(), resetData);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);

        return gameState;
    }

    /**
     * 强制结束游戏（当所有玩家离线时调用）
     */
    public void forceEndGame(GameRoom room, String reason) {

        // 取消所有定时器
        cancelRobTimeout(room.getRoomId());
        cancelPlayTimeout(room.getRoomId());

        // 设置房间状态为结束
        room.setState(RoomStateEnum.ENDING);

        // 取消所有机器人的托管
        for (GamePlayer player : room.getOrderedPlayers()) {
            if (player.isRobotControlled()) {
                player.setRobotControlled(false);
            }
        }

        // 广播强制结束消息
        ActionResultResp actionResult = ActionResultResp.builder()
                .event(GameActionEnum.GAME_FORCE_END.getCode())
                .phase(GamePhaseEnum.ENDING)
                .roomState(RoomStateEnum.ENDING)
                .message(reason)
                .build();
        broadcastActionResult(room, actionResult);

        // 广播最终状态
        broadcastGameState(room, null);

        // 同步房间状态到 Redis
        roomManager.saveRoom(room);
    }

    /**
     * 重置房间为等待状态，准备下一局
     */
    public void resetRoomForNewRound(GameRoom room) {

        // 重置房间状态为等待
        room.setState(RoomStateEnum.WAITING);

        // 清空所有玩家的准备状态
        for (GamePlayer player : room.getOrderedPlayers()) {
            player.setReady(false);
            player.setRobScore(0);
            player.setCurrentPlayedCards(null);
            // 如果玩家离线但还在房间，保持离线状态
        }

        // 重置游戏相关字段
        room.setLandlordId(null);
        room.setCurrentPlayerId(null);
        room.setLastPlayerId(null);
        room.setLastPlayedCards(null);
        room.setCurrentRobPlayerId(null);
        room.setHighestRobScore(0);
        room.setLastRobPlayerId(null);
        room.setRobRoundStartPlayerId(null);
        room.setBottomCards(null);
        if (room.getRobbedPlayers() != null) {
            room.getRobbedPlayers().clear();
        }
        if (room.getPassedRobPlayers() != null) {
            room.getPassedRobPlayers().clear();
        }

    }

    /**
     * 构建玩家结果列表
     */
    private List<GameStateResp.PlayerResultVO> buildPlayerResults(GameRoom room, Long winnerId) {
        return room.getOrderedPlayers().stream()
                .map(p -> GameStateResp.PlayerResultVO.builder()
                        .userId(p.getUserId())
                        .userName(p.getUserName())
                        .isWinner(p.getUserId().equals(winnerId))
                        .isLandlord(p.isLandlord())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 重置房间状态
     *
     * @param winnerId  赢家ID，保留其最后出牌
     * @param lastCards 赢家最后出的牌
     */
    private void resetRoomState(GameRoom room, Long winnerId, PokerHand lastCards) {
        room.setState(RoomStateEnum.WAITING);
        room.setLandlordId(null);
        room.setCurrentPlayerId(null);
        room.setLastPlayerId(null);
        room.setBottomCards(new PokerHand());

        for (GamePlayer player : room.getOrderedPlayers()) {
            player.setReady(false);
            player.setFinished(false);

            if (player.getUserId().equals(winnerId)) {
                // 赢家：手牌清空，保留最后出牌
                player.getHand().clear();
                // lastCards 已在 handleGameOver 中设置到 room，player 的 currentPlayedCards 也保留
            } else {
                // 其他玩家：清空手牌和出牌
                player.getHand().clear();
                player.clearCurrentPlayedCards();
            }
        }
    }

    // ==================== 状态构建方法 ====================

    /**
     * 构建游戏状态
     */
    /**
     * 构建游戏状态响应
     * 注意：所有涉及手牌的地方统一按斗地主规则排序，保证前后端显示顺序一致
     */
    private GameStateResp buildGameState(GameRoom room, Long viewerId) {
        // 构建玩家状态（仅在需要时排序手牌）
        List<GameStateResp.PlayerStateVO> playerStates = buildPlayerStates(room, viewerId);

        // 构建底牌（游戏开始后所有人都能看到）
        List<GameStateResp.PokerCardVO> bottomCards = null;
        if (room.getLandlordId() != null) {
            bottomCards = GameStateResp.PokerCardVO.fromList(room.getBottomCards().getAll());
        }

        // 构建最近出牌
        PokerHand lastPlayedCards = room.getLastPlayedCards();
        List<GameStateResp.PokerCardVO> lastPlayedCardList = null;
        String lastPlayerName = null;
        String lastPatternDesc = null;

        if (lastPlayedCards != null && !lastPlayedCards.isEmpty()) {
            // 对最近出牌排序后再发送
            PokerHand sortedLastPlayed = new PokerHand(lastPlayedCards.getAll());
            PokerSorter.sortByLandlordsWithUniversal(sortedLastPlayed);
            lastPlayedCardList = GameStateResp.PokerCardVO.fromList(sortedLastPlayed.getAll());
            Long lastPlayerId = room.getLastPlayerId();
            if (lastPlayerId != null) {
                GamePlayer lastPlayer = room.getPlayer(lastPlayerId);
                if (lastPlayer != null) {
                    lastPlayerName = lastPlayer.getUserName();
                }
            }
            lastPatternDesc = PokerComparator.getPatternDescription(PokerPatternMatcher.analyze(lastPlayedCards));
        }

        // 获取当前叫分
        Integer currentRobScore = null;
        if (room.getCurrentRobPlayerId() != null) {
            GamePlayer robPlayer = room.getPlayer(room.getCurrentRobPlayerId());
            if (robPlayer != null) {
                currentRobScore = robPlayer.getRobScore();
            }
        }

        // 构建私人手牌（统一排序后再推送）
        List<GameStateResp.PokerCardVO> handCards = null;
        if (viewerId != null) {
            GamePlayer viewer = room.getPlayer(viewerId);
            if (viewer != null && viewer.getHand() != null && !viewer.getHand().isEmpty()) {
                PokerHand sortedHand = new PokerHand(viewer.getHand().getAll());
                PokerSorter.sortByLandlordsWithUniversal(sortedHand);
                handCards = GameStateResp.PokerCardVO.fromList(sortedHand.getAll());
            }
        }

        return GameStateResp.builder()
                .roomId(room.getRoomId())
                .gameType(room.getGameType())
                .roomState(room.getState())
                .phase(GamePhaseEnum.fromRoomState(room.getState()))
                .ownerId(room.getOwnerId())
                .landlordId(room.getLandlordId())
                .bottomCards(bottomCards)
                .currentPlayerId(room.getCurrentPlayerId())
                .currentRobPlayerId(room.getCurrentRobPlayerId())
                .highestRobScore(room.getHighestRobScore())
                .players(playerStates)
                .lastPlayedCards(lastPlayedCardList)
                .lastPlayerId(room.getLastPlayerId())
                .lastPlayerName(lastPlayerName)
                .lastPatternDesc(lastPatternDesc)
                .handCards(handCards)
                .build();
    }

    /**
     * 构建玩家状态列表
     * 注意：只有 viewerId 匹配的玩家才返回手牌，且手牌统一排序后返回
     */
    private List<GameStateResp.PlayerStateVO> buildPlayerStates(GameRoom room, Long viewerId) {
        return room.getOrderedPlayers().stream()
                .map(p -> {
                    GameStateResp.PlayerStateVO.PlayerStateVOBuilder builder = GameStateResp.PlayerStateVO.builder()
                            .userId(p.getUserId())
                            .userName(p.getUserName())
                            .avatar(p.getAvatar())
                            .cardCount(p.getCardCount())
                            .isLandlord(p.isLandlord())
                            .isCurrentPlayer(p.getUserId().equals(room.getCurrentPlayerId()))
                            .isCurrentRobPlayer(p.getUserId().equals(room.getCurrentRobPlayerId()))
                            .isReady(p.isReady())
                            .isOnline(p.isOnline())
                            .isRobotControlled(p.isRobotControlled())
                            .robScore(p.getRobScore())
                            .role(p.getRole() != null ? p.getRole().name() : "PLAYER")
                            .currentPlayedCards(GameStateResp.PokerCardVO.fromList(p.getCurrentPlayedCards()));

                    // 只给拥有者显示手牌，且统一排序
                    if (p.getUserId().equals(viewerId)) {
                        PokerHand sortedHand = new PokerHand(p.getHand().getAll());
                        PokerSorter.sortByLandlordsWithUniversal(sortedHand);
                        builder.cards(GameStateResp.PokerCardVO.fromList(sortedHand.getAll()));
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    // ==================== 消息发送方法 ====================

    /**
     * 发送私人状态给用户
     * 注意：buildGameState 内部已统一排序，此处直接使用其返回值即可
     */
    private void sendPrivateState(GameRoom room, GamePlayer player, Long userId) {
        GameStateResp state = buildGameState(room, userId);

        WSBaseResp<GameStateResp> wsBaseResp = WSBaseResp.<GameStateResp>builder()
                .type(GameMessageTypeEnum.STATE_UPDATE.getType())
                .data(state)
                .build();

        sessionManager.sendToUser(player.getUserId(), JSON.toJSONString(wsBaseResp, JSONWriter.Feature.WriteLongAsString));
    }

    /**
     * 广播游戏状态给所有玩家
     */
    private void broadcastGameState(GameRoom room, Long excludeUserId) {
        List<Long> playerIds = room.getPlayerOrder();
        if (excludeUserId != null) {
            playerIds = playerIds.stream()
                    .filter(id -> !id.equals(excludeUserId))
                    .collect(Collectors.toList());
        }

        for (Long userId : playerIds) {
            sendPrivateState(room, room.getPlayer(userId), userId);
        }
    }

    // ==================== 统一格式广播方法 ====================

    /**
     * 广播游戏开始
     */
    private void broadcastGameStart(GameRoom room) {
        GamePlayer firstRobPlayer = room.getPlayer(room.getCurrentRobPlayerId());
        String message = String.format("游戏开始！%s 先叫地主", firstRobPlayer.getUserName());

        TurnNotifyResp notify = TurnNotifyResp.builder()
                .event(GameActionEnum.GAME_START.getCode())
                .phase(GamePhaseEnum.ROBBING)
                .roomState(RoomStateEnum.ROBBING)
                .phaseDesc("叫地主阶段")
                .message(message)
                .build();

        String json = JSON.toJSONString(notify, JSONWriter.Feature.WriteLongAsString);
        for (Long userId : room.getPlayerOrder()) {
            WSBaseResp<Object> wsBaseResp = WSBaseResp.<Object>builder()
                    .type(GameMessageTypeEnum.START_GAME.getType())
                    .data(JSON.parseObject(json))
                    .build();
            sessionManager.sendToUser(userId, JSON.toJSONString(wsBaseResp, JSONWriter.Feature.WriteLongAsString));
        }
    }

    /**
     * 广播回合通知 - 告诉所有人轮到谁了
     * 统一的回合开始通知格式
     */
    private void broadcastTurnNotify(GameRoom room, TurnNotifyResp notify) {
        String json = JSON.toJSONString(notify, JSONWriter.Feature.WriteLongAsString);
        for (Long userId : room.getPlayerOrder()) {
            WSBaseResp<Object> wsBaseResp = WSBaseResp.<Object>builder()
                    .type(GameMessageTypeEnum.TURN_NOTIFY.getType())
                    .data(JSON.parseObject(json))
                    .build();
            sessionManager.sendToUser(userId, JSON.toJSONString(wsBaseResp, JSONWriter.Feature.WriteLongAsString));
        }
    }

    /**
     * 广播操作结果 - 告诉所有人某个玩家做了什么
     * 统一格式
     */
    private void broadcastActionResult(GameRoom room, ActionResultResp result) {
        String json = JSON.toJSONString(result, JSONWriter.Feature.WriteLongAsString);
        for (Long userId : room.getPlayerOrder()) {
            WSBaseResp<Object> wsBaseResp = WSBaseResp.<Object>builder()
                    .type(GameMessageTypeEnum.ACTION_RESULT.getType())
                    .data(JSON.parseObject(json))
                    .build();
            sessionManager.sendToUser(userId, JSON.toJSONString(wsBaseResp, JSONWriter.Feature.WriteLongAsString));
        }
    }

    /**
     * 广播玩家进入/退出托管状态（使用 ACTION_RESULT 消息）
     */
    private void broadcastPlayerRobotStatus(GameRoom room, GamePlayer player) {
        GameActionEnum event;
        String message;
        if (player.isRobotControlled()) {
            RobotReasonEnum reason = player.getRobotReason();
            String reasonDesc = reason == RobotReasonEnum.TIMEOUT ? "超时" :
                    (reason == RobotReasonEnum.LEAVE ? "离开" : "主动托管");
            event = GameActionEnum.ROBOT_ENABLED;
            message = String.format("%s [%s]，AI托管中", player.getUserName(), reasonDesc);
        } else {
            event = GameActionEnum.ROBOT_DISABLED;
            message = String.format("%s 取消了AI托管", player.getUserName());
        }

        ActionResultResp actionResult = ActionResultResp.builder()
                .event(event.getCode())
                .phase(GamePhaseEnum.fromRoomState(room.getState()))
                .roomState(room.getState())
                .playerId(player.getUserId())
                .playerName(player.getUserName())
                .action(GameActionEnum.ROBOT.getCode())
                .result(message)
                .message(message)
                .build();

        broadcastActionResult(room, actionResult);
    }

    /**
     * 取消AI托管
     */
    public void cancelRobotControl(GameRoom room, Long userId) {
        GamePlayer player = room.getPlayer(userId);
        if (player != null && player.isRobotControlled()) {
            player.setRobotControlled(false);
            player.setRobotReason(null);
            broadcastPlayerRobotStatus(room, player);
            // 同步房间状态到 Redis
            roomManager.saveRoom(room);
        }
    }

    /**
     * 设置AI托管（主动托管或离开托管）
     * 如果当前玩家就是操作玩家，立即触发AI操作
     */
    public void setRobotControl(GameRoom room, Long userId, RobotReasonEnum reason) {
        GamePlayer player = room.getPlayer(userId);
        if (player != null && !player.isRobotControlled()) {
            player.setRobotControlled(true);
            player.setRobotReason(reason);
            broadcastPlayerRobotStatus(room, player);
            // 同步房间状态到 Redis
            roomManager.saveRoom(room);

            // 如果是当前操作玩家，立即触发AI操作
            boolean isCurrentRobPlayer = room.getState() == RoomStateEnum.ROBBING
                    && room.getCurrentRobPlayerId() != null
                    && room.getCurrentRobPlayerId().equals(userId);
            boolean isCurrentPlayPlayer = room.getState() == RoomStateEnum.PLAYING
                    && room.getCurrentPlayerId() != null
                    && room.getCurrentPlayerId().equals(userId);

            if (isCurrentRobPlayer) {
                // 立即执行AI叫分
                int aiRobScore = robotService.getRobScore();
                robLandlord(room, userId, aiRobScore);
            } else if (isCurrentPlayPlayer) {
                // 立即执行AI出牌
                executeRobotPlay(room, userId);
            }
        }
    }

    /**
     * 广播叫地主回合通知
     */
    private void broadcastRobTurnNotify(GameRoom room, Long currentRobPlayerId) {
        GamePlayer player = room.getPlayer(currentRobPlayerId);

        // 构建可选操作
        List<TurnNotifyResp.ActionOption> options = new ArrayList<>();
        int maxScore = Math.min(3, room.getHighestRobScore() + 1);
        for (int i = 0; i <= maxScore; i++) {
            boolean enabled = i == 0 || i > room.getHighestRobScore();
            options.add(TurnNotifyResp.ActionOption.builder()
                    .value(i)
                    .name(i == 0 ? "不叫" : i + "分")
                    .enabled(enabled)
                    .hint(enabled ? "" : "分数太低")
                    .build());
        }

        String message = String.format("请 %s 叫地主 (当前最高 %d 分)",
                player.getUserName(), room.getHighestRobScore());

        TurnNotifyResp notify = TurnNotifyResp.builder()
                .event(GameActionEnum.TURN_START.getCode())
                .phase(GamePhaseEnum.ROBBING)
                .roomState(RoomStateEnum.ROBBING)
                .phaseDesc("叫地主阶段")
                .currentPlayerId(currentRobPlayerId)
                .currentPlayerName(player.getUserName())
                .action(GameActionEnum.ROB.getCode())
                .actionOptions(options)
                .canPass(true)
                .canPlay(false)
                .timeout((int) (GameConstants.ROB_TIMEOUT / 1000))
                .startTime(System.currentTimeMillis())
                .highestScore(room.getHighestRobScore())
                .message(message)
                .build();

        broadcastTurnNotify(room, notify);
    }

    /**
     * 广播出牌回合通知
     */
    private void broadcastPlayTurnNotify(GameRoom room, Long currentPlayerId, boolean canPass) {
        GamePlayer player = room.getPlayer(currentPlayerId);
        String message = canPass
                ? String.format("请 %s 出牌或选择不出", player.getUserName())
                : String.format("请 %s 出牌", player.getUserName());

        // 如果是托管玩家，延迟2秒后执行AI操作
        if (player.isRobotControlled()) {
            log.info("轮到托管玩家出牌，延迟2秒执行AI: playerId={}", currentPlayerId);

            // 广播回合通知（让前端显示倒计时）
            TurnNotifyResp notify = TurnNotifyResp.builder()
                    .event(GameActionEnum.TURN_START.getCode())
                    .phase(GamePhaseEnum.PLAYING)
                    .roomState(RoomStateEnum.PLAYING)
                    .phaseDesc("出牌阶段")
                    .currentPlayerId(currentPlayerId)
                    .currentPlayerName(player.getUserName())
                    .action(GameActionEnum.PLAY.getCode())
                    .canPass(canPass)
                    .canPlay(true)
                    .timeout((int) (GameConstants.PLAY_TIMEOUT / 1000))
                    .startTime(System.currentTimeMillis())
                    .message(message)
                    .build();

            broadcastTurnNotify(room, notify);

            // 延迟2秒后执行AI出牌
            scheduler.schedule(() -> {
                log.info("AI延迟出牌开始: playerId={}", currentPlayerId);
                executeRobotPlay(room, currentPlayerId);
            }, 2, TimeUnit.SECONDS);

            return;
        }

        TurnNotifyResp notify = TurnNotifyResp.builder()
                .event(GameActionEnum.TURN_START.getCode())
                .phase(GamePhaseEnum.PLAYING)
                .roomState(RoomStateEnum.PLAYING)
                .phaseDesc("出牌阶段")
                .currentPlayerId(currentPlayerId)
                .currentPlayerName(player.getUserName())
                .action(GameActionEnum.PLAY.getCode())
                .canPass(canPass)
                .canPlay(true)
                .timeout((int) (GameConstants.PLAY_TIMEOUT / 1000))
                .startTime(System.currentTimeMillis())
                .message(message)
                .build();

        broadcastTurnNotify(room, notify);

        // 启动出牌超时
        startPlayTimeout(room);
    }

    // ==================== 工具方法 ====================

    /**
     * 广播消息给房间内所有玩家
     */
    private void broadcastToRoom(GameRoom room, String type, Object data) {
        for (Long userId : room.getPlayerOrder()) {
            sessionManager.sendToUser(userId, buildWSMessage(type, data));
        }
    }

    /**
     * 构建 WebSocket 消息
     */
    private String buildWSMessage(String type, Object data) {
        WSBaseResp<Object> wsBaseResp = WSBaseResp.builder()
                .type(type)
                .data(data)
                .build();
        return JSON.toJSONString(wsBaseResp, JSONWriter.Feature.WriteLongAsString);
    }

    // ==================== 超时管理 ====================

    /**
     * 启动叫地主超时
     */
    private void startRobTimeout(GameRoom room) {
        String roomId = room.getRoomId();
        cancelRobTimeout(roomId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (room.getState() == RoomStateEnum.ROBBING) {
                try {
                    Long currentPlayerId = room.getCurrentRobPlayerId();
                    GamePlayer player = room.getPlayer(currentPlayerId);

                    // 设置AI托管状态
                    player.setRobotControlled(true);
                    player.setRobotReason(RobotReasonEnum.TIMEOUT);

                    // 广播玩家进入托管状态
                    broadcastPlayerRobotStatus(room, player);

                    // AI叫分（始终不叫）
                    int aiRobScore = robotService.getRobScore();

                    robLandlord(room, currentPlayerId, aiRobScore);
                } catch (GameBusinessException e) {
                    log.error("叫地主超时处理失败: {}", e.getMessage());
                }
            }
        }, GameConstants.ROB_TIMEOUT, TimeUnit.MILLISECONDS);

        robTimeoutTasks.put(roomId, future);
    }

    /**
     * 启动出牌超时
     */
    private void startPlayTimeout(GameRoom room) {
        String roomId = room.getRoomId();
        cancelPlayTimeout(roomId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            if (room.getState() == RoomStateEnum.PLAYING) {
                try {
                    Long currentPlayerId = room.getCurrentPlayerId();
                    GamePlayer currentPlayer = room.getPlayer(currentPlayerId);

                    if (currentPlayer.getHand() == null || currentPlayer.getHand().isEmpty()) {
                        log.error("出牌超时: 玩家手牌为空");
                        return;
                    }

                    // 设置AI托管状态
                    currentPlayer.setRobotControlled(true);
                    currentPlayer.setRobotReason(RobotReasonEnum.TIMEOUT);

                    // 广播玩家进入托管状态
                    broadcastPlayerRobotStatus(room, currentPlayer);

                    // 执行AI出牌
                    executeRobotPlay(room, currentPlayerId);
                } catch (GameBusinessException e) {
                    log.error("出牌超时处理失败: {}", e.getMessage());
                }
            }
        }, GameConstants.PLAY_TIMEOUT, TimeUnit.MILLISECONDS);

        playTimeoutTasks.put(roomId, future);
    }

    /**
     * 取消叫地主超时
     */
    private void cancelRobTimeout(String roomId) {
        ScheduledFuture<?> task = robTimeoutTasks.remove(roomId);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 取消出牌超时
     */
    private void cancelPlayTimeout(String roomId) {
        ScheduledFuture<?> task = playTimeoutTasks.remove(roomId);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 执行AI出牌（托管或超时后）
     */
    private void executeRobotPlay(GameRoom room, Long playerId) {
        GamePlayer player = room.getPlayer(playerId);
        PokerHand hand = player.getHand();

        if (hand == null || hand.isEmpty()) {
            log.error("AI出牌: 玩家手牌为空");
            return;
        }

        // 判断下一个玩家是否可以不出
        boolean canPass = room.getLastPlayerId() != null && !room.getLastPlayerId().equals(playerId);

        if (canPass) {
            // 有上家出过牌，AI尝试压牌
            List<String> playCards = robotService.getPlayCards(room, playerId);
            if (playCards.isEmpty()) {
                log.info("AI托管无法压牌: playerId={}", playerId);
                pass(room, playerId);
            } else {
                log.info("AI托管出牌: playerId={}, cards={}", playerId, playCards);
                playCards(room, playerId, playCards);
            }
        } else {
            // 第一个出牌，出最小的一张
            PokerSorter.sortByLandlords(hand);
            Poker smallestCard = hand.getAll().get(hand.getAll().size() - 1); // 最小牌在最后
            String cardId = smallestCard.getId();
            log.info("AI托管出最小牌: playerId={}, card={}", playerId, cardId);
            playCards(room, playerId, Collections.singletonList(cardId));
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 验证玩家数量
     */
    private void validatePlayerCount(GameRoom room) {
        if (room.getPlayerCount() < GameConstants.LANDLORDS_PLAYERS) {
            throw new GameBusinessException("INVALID_PLAYER_COUNT",
                    String.format("斗地主需要至少%d名玩家", GameConstants.LANDLORDS_PLAYERS));
        }
    }

    /**
     * 解析扑克牌
     */
    private List<Poker> parsePokers(List<String> pokerIds) {
        return pokerIds.stream()
                .map(PokerGenerator::parseById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 检查手牌是否包含指定牌（按面值比较）
     * 用于解决解析牌时花色可能与实际手牌不一致的问题
     */
    private boolean containsCardByValue(PokerHand hand, Poker target) {
        if (hand == null || target == null) {
            return false;
        }
        for (Poker p : hand.getAll()) {
            if (p.getValue() == target.getValue()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从手牌中移除指定牌（按面值比较）
     * 用于解决解析牌时花色可能与实际手牌不一致的问题
     */
    private boolean removeCardByValue(PokerHand hand, Poker target) {
        if (hand == null || target == null) {
            return false;
        }
        for (Poker p : hand.getAll()) {
            if (p.getValue() == target.getValue()) {
                hand.remove(p);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取玩家手牌
     */
    public List<String> getPlayerHand(GameRoom room, Long userId) {
        GamePlayer player = room.getPlayer(userId);
        if (player != null && player.getHand() != null) {
            return player.getHand().toIdList();
        }
        return null;
    }

    /**
     * 清理房间相关的所有定时任务
     *
     * @param roomId 房间ID
     */
    public void cleanupRoomTasks(String roomId) {
        // 取消叫地主超时（内部会从 Map 中移除）
        cancelRobTimeout(roomId);
        // 取消出牌超时（内部会从 Map 中移除）
        cancelPlayTimeout(roomId);
    }

    // ==================== 游戏验证结果 ====================

    /**
     * 游戏验证结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class GameValidationResult {
        private boolean valid;
        private String errorCode;
        private String errorMessage;

        public static GameValidationResult valid() {
            return new GameValidationResult(true, null, null);
        }

        public static GameValidationResult invalid(String errorCode, String errorMessage) {
            return new GameValidationResult(false, errorCode, errorMessage);
        }
    }
}
