package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI托管原因枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum RobotReasonEnum {

    /**
     * 操作超时
     */
    TIMEOUT("timeout", "超时"),

    /**
     * 玩家离开（游戏中）
     */
    LEAVE("leave", "离开"),

    /**
     * 玩家主动托管
     */
    MANUAL("manual", "主动托管");

    private final String code;
    private final String description;

    public static RobotReasonEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RobotReasonEnum reason : values()) {
            if (reason.code.equals(code)) {
                return reason;
            }
        }
        return null;
    }
}
