package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 玩家角色枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum PlayerRoleEnum {

    PLAYER(0, "玩家", "普通玩家"),
    OWNER(1, "房主", "房间创建者");

    private final int code;
    private final String name;
    private final String description;

    public static PlayerRoleEnum getByCode(int code) {
        for (PlayerRoleEnum role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return PLAYER;
    }

    public boolean canStartGame() {
        return this == OWNER;
    }

    public boolean canKick() {
        return this == OWNER;
    }
}
