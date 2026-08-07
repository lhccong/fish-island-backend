package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 玩家在线状态枚举
 * 用于房间内玩家状态变更广播
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum PlayerStatusEnum {

    /**
     * 在线
     */
    ONLINE("online", "在线"),

    /**
     * 离线
     */
    OFFLINE("offline", "离线"),

    /**
     * 重连中
     */
    RECONNECTING("reconnecting", "重连中");

    private final String code;
    private final String description;

    public static PlayerStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (PlayerStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
