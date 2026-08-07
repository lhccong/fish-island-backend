package com.cong.fishisland.game.enums;

import com.cong.fishisland.game.landlords.enums.GamePhaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 房间状态枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum RoomStateEnum {

    WAITING(1, "等待中", "等待玩家加入或准备"),
    READY(2, "已准备", "玩家已准备，等待开始"),
    DISTRIBUTING(3, "发牌中", "游戏正在发牌"),
    ROBBING(4, "叫地主", "斗地主叫地主阶段"),
    PLAYING(5, "游戏中", "游戏进行中"),
    ENDING(6, "结束中", "游戏结束，结算中"),
    CLOSED(0, "已关闭", "房间已关闭");

    private final int code;
    private final String name;
    private final String description;

    public static RoomStateEnum getByCode(int code) {
        for (RoomStateEnum state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        return null;
    }

    /**
     * 是否可以加入房间
     */
    public boolean canJoin() {
        return this == WAITING;
    }

    /**
     * 是否可以开始游戏
     */
    public boolean canStart() {
        return this == WAITING || this == READY;
    }

    /**
     * 是否游戏进行中
     */
    public boolean isPlaying() {
        return this == ROBBING || this == PLAYING;
    }

    /**
     * 从 GamePhaseEnum 转换
     * @deprecated 推荐在 service 层直接根据业务语义填充 roomState/phase，避免两个枚举互相耦合。
     */
    @Deprecated
    public static RoomStateEnum fromGamePhase(GamePhaseEnum phase) {
        if (phase == null) {
            return WAITING;
        }
        switch (phase) {
            case DEALING:
                return DISTRIBUTING;
            case ROBBING:
                return ROBBING;
            case LANDLORD_CONFIRMED:
            case PLAYING:
                return PLAYING;
            case ENDING:
                return ENDING;
            case CLOSED:
                return CLOSED;
            case WAITING:
            default:
                return WAITING;
        }
    }
}
