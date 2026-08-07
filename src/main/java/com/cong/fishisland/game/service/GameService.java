package com.cong.fishisland.game.service;

import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.landlords.dto.response.GameStateResp;
import com.cong.fishisland.game.model.room.GameRoom;

import java.util.List;

/**
 * 游戏服务接口
 *
 * @author cong
 */
public interface GameService {

    /**
     * 获取游戏类型
     */
    GameTypeEnum getGameType();

    /**
     * 开始游戏
     */
    GameStateResp startGame(GameRoom room);

    /**
     * 叫地主
     *
     * @param room   房间
     * @param userId 玩家ID
     * @param action 叫分动作 (0-不叫, 1-1分, 2-2分, 3-3分)
     * @return 游戏状态
     */
    GameStateResp robLandlord(GameRoom room, Long userId, Integer action);

    /**
     * 出牌
     *
     * @param room   房间
     * @param userId 玩家ID
     * @param pokers 出的牌
     * @return 游戏状态
     */
    GameStateResp playCards(GameRoom room, Long userId, List<String> pokers);

    /**
     * 不出
     *
     * @param room   房间
     * @param userId 玩家ID
     * @return 游戏状态
     */
    GameStateResp pass(GameRoom room, Long userId);

    /**
     * 获取当前游戏状态
     */
    GameStateResp getGameState(GameRoom room);

    /**
     * 获取当前游戏状态（带查看者ID，用于权限控制）
     */
    GameStateResp getGameState(GameRoom room, Long viewerId);

    /**
     * 玩家断线重连
     */
    void reconnect(GameRoom room, Long userId);

    /**
     * 玩家离线
     */
    void disconnect(GameRoom room, Long userId);
}
