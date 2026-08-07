package com.cong.fishisland.game.landlords.dto.response;

import com.cong.fishisland.game.enums.GameActionEnum;
import com.cong.fishisland.game.landlords.enums.GamePhaseEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 回合通知响应
 * 统一格式：告诉所有人轮到谁了，以及该玩家可以做什么操作
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnNotifyResp {

    // ==================== 事件类型 ====================

    /**
     * 事件类型
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

    /**
     * 阶段描述
     */
    private String phaseDesc;

    // ==================== 当前操作者信息 ====================

    /**
     * 当前操作玩家ID
     */
    private Long currentPlayerId;

    /**
     * 当前操作玩家名称
     */
    private String currentPlayerName;

    /**
     * 当前操作玩家是否是自己
     */
    private Boolean isCurrentPlayerMe;

    // ==================== 操作选项 ====================

    /**
     * 操作类型
     * @see com.cong.fishisland.game.enums.GameActionEnum
     */
    private String action;

    /**
     * 可选操作列表
     */
    private List<ActionOption> actionOptions;

    /**
     * 是否可以跳过/不出
     */
    private Boolean canPass;

    /**
     * 是否可以出牌
     */
    private Boolean canPlay;

    // ==================== 超时信息 ====================

    /**
     * 超时时间(秒)
     */
    private Integer timeout;

    /**
     * 计时开始时间戳(毫秒) - 前端用于同步倒计时
     */
    private Long startTime;

    /**
     * 提示信息
     */
    private String message;

    // ==================== 额外信息 ====================

    /**
     * 最高分(叫地主阶段)
     */
    private Integer highestScore;

    /**
     * 地主ID(确定地主后)
     */
    private Long landlordId;

    /**
     * 地主名称(确定地主后)
     */
    private String landlordName;

    // ==================== 内部类 ====================

    /**
     * 操作选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionOption {
        /**
         * 操作值
         */
        private Integer value;

        /**
         * 操作名称
         */
        private String name;

        /**
         * 是否可选
         */
        private Boolean enabled;

        /**
         * 提示文本
         */
        private String hint;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建叫地主回合通知
     */
    public static TurnNotifyResp forRobbing(Long currentPlayerId, String currentPlayerName,
                                             Integer highestScore, Integer timeout,
                                             List<ActionOption> options, String message) {
        return TurnNotifyResp.builder()
                .event(GameActionEnum.TURN_START.getCode())
                .phase(GamePhaseEnum.ROBBING)
                .roomState(RoomStateEnum.ROBBING)
                .phaseDesc("叫地主阶段")
                .currentPlayerId(currentPlayerId)
                .currentPlayerName(currentPlayerName)
                .action(GameActionEnum.ROB.getCode())
                .actionOptions(options)
                .canPass(true)
                .canPlay(false)
                .timeout(timeout)
                .highestScore(highestScore)
                .message(message)
                .build();
    }

    /**
     * 创建出牌回合通知
     */
    public static TurnNotifyResp forPlaying(Long currentPlayerId, String currentPlayerName,
                                             Integer timeout, Boolean canPass, String message) {
        return TurnNotifyResp.builder()
                .event(GameActionEnum.TURN_START.getCode())
                .phase(GamePhaseEnum.PLAYING)
                .roomState(RoomStateEnum.PLAYING)
                .phaseDesc("出牌阶段")
                .currentPlayerId(currentPlayerId)
                .currentPlayerName(currentPlayerName)
                .action(GameActionEnum.PLAY.getCode())
                .canPass(canPass)
                .canPlay(true)
                .timeout(timeout)
                .message(message)
                .build();
    }

    /**
     * 创建阶段变化通知
     */
    public static TurnNotifyResp forPhaseChange(GamePhaseEnum phase, String message) {
        return TurnNotifyResp.builder()
                .event(GameActionEnum.PHASE_CHANGE.getCode())
                .phase(phase)
                .roomState(RoomStateEnum.fromGamePhase(phase))
                .phaseDesc(getPhaseDesc(phase))
                .message(message)
                .build();
    }

    /**
     * 创建地主确定通知
     */
    public static TurnNotifyResp forLandlordConfirmed(Long landlordId, String landlordName,
                                                      Integer timeout, String message) {
        return TurnNotifyResp.builder()
                .event(GameActionEnum.TURN_START.getCode())
                .phase(GamePhaseEnum.PLAYING)
                .roomState(RoomStateEnum.PLAYING)
                .phaseDesc("出牌阶段")
                .currentPlayerId(landlordId)
                .currentPlayerName(landlordName)
                .action(GameActionEnum.PLAY.getCode())
                .canPass(false)
                .canPlay(true)
                .timeout(timeout)
                .landlordId(landlordId)
                .landlordName(landlordName)
                .message(message)
                .build();
    }

    /**
     * 获取阶段描述
     */
    private static String getPhaseDesc(GamePhaseEnum phase) {
        if (phase == null) return "未知阶段";
        switch (phase) {
            case WAITING: return "等待阶段";
            case ROBBING: return "叫地主阶段";
            case PLAYING: return "出牌阶段";
            case ENDING: return "结束阶段";
            default: return phase.name();
        }
    }
}
