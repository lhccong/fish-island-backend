package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游戏操作/事件统一枚举
 * <p>
 * 1. 玩家可执行的动作（ROB / PLAY / PASS / DOUBLE 等）
 * 2. 系统级事件（GAME_START / GAME_OVER / ROB_RESULT / PLAY_RESULT ...）
 * 3. 房间级事件（playerJoin / playerLeave / playerStatusChange）
 * 4. AI 托管事件（ROBOT_ENABLED / ROBOT_DISABLED）
 * <p>
 * 之前 GameEventEnum 和 GameActionEnum 是一对一镜像，统一枚举可避免序列化两端字段冗余。
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum GameActionEnum {

    // ==================== 玩家操作 ====================
    ROB("ROB", "叫地主", "玩家操作"),
    PLAY("PLAY", "出牌", "玩家操作"),
    PASS("PASS", "不出", "玩家操作"),
    LANDLORD("LANDLORD", "成为地主", "系统行为"),
    DOUBLE("DOUBLE", "加倍", "玩家操作（预留）"),

    // ==================== 回合通知事件 ====================
    TURN_START("TURN_START", "回合开始", "回合通知"),
    TURN_END("TURN_END", "回合结束", "回合通知"),
    PHASE_CHANGE("PHASE_CHANGE", "阶段变化", "回合通知"),

    // ==================== 游戏流程事件 ====================
    GAME_START("GAME_START", "游戏开始", "游戏流程"),
    GAME_OVER("GAME_OVER", "游戏结束", "游戏流程"),
    GAME_FORCE_END("GAME_FORCE_END", "强制结束", "游戏流程"),

    // ==================== 操作结果事件 ====================
    ROB_RESULT("ROB_RESULT", "叫地主结果", "操作结果"),
    PLAY_RESULT("PLAY_RESULT", "出牌结果", "操作结果"),
    PASS_RESULT("PASS_RESULT", "不出结果", "操作结果"),
    LANDLORD_CONFIRMED("LANDLORD_CONFIRMED", "地主确定", "操作结果"),

    // ==================== 房间事件 ====================
    PLAYER_JOIN("playerJoin", "玩家加入", "房间事件"),
    PLAYER_LEAVE("playerLeave", "玩家离开", "房间事件"),
    PLAYER_STATUS_CHANGE("playerStatusChange", "玩家状态变化", "房间事件"),
    PLAYER_RECONNECT("playerReconnect", "玩家重连", "房间事件"),

    // ==================== AI托管 ====================
    ROBOT("ROBOT", "AI托管", "AI托管"),
    ROBOT_ENABLED("ROBOT_ENABLED", "AI托管开启", "AI托管"),
    ROBOT_DISABLED("ROBOT_DISABLED", "AI托管关闭", "AI托管");

    private final String code;
    private final String name;
    private final String category;

    public static GameActionEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (GameActionEnum action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }

    /**
     * 是否是回合通知事件
     */
    public boolean isTurnEvent() {
        return this == TURN_START || this == TURN_END || this == PHASE_CHANGE;
    }

    /**
     * 是否是操作结果事件
     */
    public boolean isActionResult() {
        return this == ROB_RESULT || this == PLAY_RESULT || this == PASS_RESULT
                || this == LANDLORD_CONFIRMED || this == GAME_OVER
                || this == ROBOT_ENABLED || this == ROBOT_DISABLED;
    }

    /**
     * 是否是房间事件
     */
    public boolean isRoomEvent() {
        return this == PLAYER_JOIN || this == PLAYER_LEAVE || this == PLAYER_STATUS_CHANGE || this == PLAYER_RECONNECT;
    }

    /**
     * 是否是玩家动作（用于回合通知时的 action 字段）
     */
    public boolean isPlayerAction() {
        return this == ROB || this == PLAY || this == PASS || this == LANDLORD || this == DOUBLE;
    }
}
