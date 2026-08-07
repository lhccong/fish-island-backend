package com.cong.fishisland.game.landlords.dto.response;

import com.cong.fishisland.game.enums.GameActionEnum;
import com.cong.fishisland.game.landlords.enums.GamePhaseEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作结果响应
 * 统一格式：告诉所有人某个玩家做了什么操作
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResultResp {

    // ==================== 事件类型 ====================

    /**
     * 事件类型
     *
     * @see com.cong.fishisland.game.enums.GameActionEnum
     */
    private String event;

    // ==================== 阶段信息 ====================

    /**
     * 当前阶段
     */
    private GamePhaseEnum phase;

    /**
     * 房间状态
     */
    private RoomStateEnum roomState;

    // ==================== 玩家信息 ====================

    /**
     * 操作玩家ID
     */
    private Long playerId;

    /**
     * 操作玩家名称
     */
    private String playerName;

    // ==================== 操作结果 ====================

    /**
     * 操作类型
     *
     * @see com.cong.fishisland.game.enums.GameActionEnum
     */
    private String action;

    /**
     * 操作值
     * - 叫地主: 0(不叫), 1, 2, 3
     * - 出牌: null
     */
    private Integer actionValue;

    /**
     * 操作结果描述
     */
    private String result;

    /**
     * 提示消息
     */
    private String message;

    // ==================== 出牌相关信息 ====================

    /**
     * 出牌扑克牌ID列表
     */
    private List<String> pokerIds;

    /**
     * 牌型描述
     */
    private String patternDesc;

    /**
     * 是否是炸弹
     */
    private Boolean isBomb;

    /**
     * 是否是最大牌(癞子模式)
     */
    private Boolean isMaxCard;

    // ==================== 叫地主相关信息 ====================

    /**
     * 当前最高分
     */
    private Integer highestScore;

    /**
     * 叫分结果描述(如 "2分", "不叫")
     */
    private String robScoreDesc;

    // ==================== 游戏结果 ====================

    /**
     * 地主ID(确定地主后)
     */
    private Long landlordId;

    /**
     * 地主名称(确定地主后)
     */
    private String landlordName;

    /**
     * 获胜者ID(游戏结束时)
     */
    private Long winnerId;

    /**
     * 获胜者名称(游戏结束时)
     */
    private String winnerName;

    /**
     * 地主是否获胜(游戏结束时)
     */
    private Boolean isLandlordWin;

    /**
     * 获胜者队伍(游戏结束时)
     */
    private String winTeam;

    /**
     * 玩家结果列表(游戏结束时)
     */
    private List<PlayerResultVO> players;

    // ==================== 静态工厂方法 ====================

    /**
     * 创建叫地主结果
     */
    public static ActionResultResp robResult(Long playerId, String playerName,
                                             Integer actionValue, String robScoreDesc,
                                             Integer highestScore, String message) {
        return ActionResultResp.builder()
                .event(GameActionEnum.ROB_RESULT.getCode())
                .phase(GamePhaseEnum.ROBBING)
                .roomState(RoomStateEnum.ROBBING)
                .playerId(playerId)
                .playerName(playerName)
                .action(GameActionEnum.ROB.getCode())
                .actionValue(actionValue)
                .result(robScoreDesc)
                .message(message)
                .highestScore(highestScore)
                .robScoreDesc(robScoreDesc)
                .build();
    }

    /**
     * 创建出牌结果
     */
    public static ActionResultResp playResult(Long playerId, String playerName,
                                              List<Poker> pokers, String patternDesc,
                                              Boolean isBomb, String message) {
        List<String> pokerIds = pokers.stream()
                .map(Poker::getId)
                .collect(Collectors.toList());

        return ActionResultResp.builder()
                .event(GameActionEnum.PLAY_RESULT.getCode())
                .phase(GamePhaseEnum.PLAYING)
                .roomState(RoomStateEnum.PLAYING)
                .playerId(playerId)
                .playerName(playerName)
                .action(GameActionEnum.PLAY.getCode())
                .result(patternDesc)
                .message(message)
                .pokerIds(pokerIds)
                .patternDesc(patternDesc)
                .isBomb(isBomb)
                .build();
    }

    /**
     * 创建不出结果
     */
    public static ActionResultResp passResult(Long playerId, String playerName, String message) {
        return ActionResultResp.builder()
                .event(GameActionEnum.PASS_RESULT.getCode())
                .phase(GamePhaseEnum.PLAYING)
                .roomState(RoomStateEnum.PLAYING)
                .playerId(playerId)
                .playerName(playerName)
                .action(GameActionEnum.PASS.getCode())
                .result("不出")
                .message(message)
                .build();
    }

    /**
     * 创建地主确定结果
     */
    public static ActionResultResp landlordConfirmed(Long landlordId, String landlordName,
                                                     List<Poker> bottomCards, String message) {
        List<String> bottomPokerIds = bottomCards.stream()
                .map(Poker::getId)
                .collect(Collectors.toList());

        return ActionResultResp.builder()
                .event(GameActionEnum.LANDLORD_CONFIRMED.getCode())
                .phase(GamePhaseEnum.PLAYING)
                .roomState(RoomStateEnum.PLAYING)
                .playerId(landlordId)
                .playerName(landlordName)
                .action(GameActionEnum.LANDLORD.getCode())
                .result(landlordName + "成为地主")
                .message(message)
                .landlordId(landlordId)
                .landlordName(landlordName)
                .pokerIds(bottomPokerIds)
                .build();
    }

    /**
     * 创建游戏结束结果
     */
    public static ActionResultResp gameOver(Long winnerId, String winnerName,
                                            Boolean isLandlordWin, String winTeam,
                                            List<PlayerResultVO> players, String message) {
        return ActionResultResp.builder()
                .event(GameActionEnum.GAME_OVER.getCode())
                .phase(GamePhaseEnum.ENDING)
                .roomState(RoomStateEnum.ENDING)
                .winnerId(winnerId)
                .winnerName(winnerName)
                .isLandlordWin(isLandlordWin)
                .winTeam(winTeam)
                .result(message)
                .message(message)
                .players(players)
                .build();
    }

    /**
     * 玩家结果VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResultVO {
        private Long userId;
        private String userName;
        private Boolean isWinner;
        private Boolean isLandlord;
        private Integer scoreChange;
    }
}
