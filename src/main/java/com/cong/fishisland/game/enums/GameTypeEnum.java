package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游戏类型枚举
 * <p>
 * 当前已实现的游戏类型。新增游戏时只需新增枚举项并实现 {@link com.cong.fishisland.game.service.GameService} 接口即可。
 * <p>
 * 注意：未实现的游戏类型（如德州、麻将等）已下线，避免内部反复维护旧值。等真实接入新游戏时再加回。
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum GameTypeEnum {

    /**
     * 经典斗地主
     */
    LANDLORDS_CLASSIC(1, "斗地主", "经典版斗地主"),

    /**
     * 未知/不支持的游戏类型
     */
    UNKNOWN(0, "未知", "未知游戏类型");

    private final int code;
    private final String name;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static GameTypeEnum getByCode(int code) {
        for (GameTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * 是否是斗地主
     */
    public boolean isLandlords() {
        return this == LANDLORDS_CLASSIC;
    }

    /**
     * 斗地主玩家数（其他人扩展时新增方法或重写）
     */
    public int getPlayerCount() {
        return this == LANDLORDS_CLASSIC ? 3 : 0;
    }

    /**
     * 最少玩家数
     */
    public int getMinPlayers() {
        return this == LANDLORDS_CLASSIC ? 3 : 2;
    }

    /**
     * 最多玩家数
     */
    public int getMaxPlayers() {
        return this == LANDLORDS_CLASSIC ? 6 : 6;
    }
}
