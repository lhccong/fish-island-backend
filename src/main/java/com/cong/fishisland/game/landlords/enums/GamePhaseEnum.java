package com.cong.fishisland.game.landlords.enums;

import com.cong.fishisland.game.enums.RoomStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 游戏阶段枚举
 * 描述斗地主游戏的完整生命周期
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum GamePhaseEnum {

    /**
     * 等待阶段 - 等待玩家加入和准备
     */
    WAITING("waiting", "等待中", "等待玩家加入或准备"),

    /**
     * 发牌阶段 - 游戏开始，发牌动画中
     */
    DEALING("dealing", "发牌中", "游戏正在发牌"),

    /**
     * 叫地主阶段 - 玩家轮流叫地主
     */
    ROBBING("robbing", "叫地主", "斗地主叫地主阶段"),

    /**
     * 确定地主阶段 - 地主确定，揭示底牌
     */
    LANDLORD_CONFIRMED("landlord_confirmed", "地主确定", "地主已确定，底牌已分配"),

    /**
     * 出牌阶段 - 游戏进行中
     */
    PLAYING("playing", "出牌中", "游戏进行中"),

    /**
     * 结束阶段 - 游戏结束，结算中
     */
    ENDING("ending", "结束中", "游戏结束，结算中"),

    /**
     * 已关闭 - 房间已关闭
     */
    CLOSED("closed", "已关闭", "房间已关闭");

    private final String code;
    private final String name;
    private final String description;

    /**
     * 根据 code 获取枚举
     */
    public static GamePhaseEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (GamePhaseEnum phase : values()) {
            if (phase.code.equals(code)) {
                return phase;
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
        return this == WAITING;
    }

    /**
     * 是否游戏进行中
     */
    public boolean isPlaying() {
        return this == DEALING || this == ROBBING || this == LANDLORD_CONFIRMED || this == PLAYING;
    }

    /**
     * 是否是叫地主阶段
     */
    public boolean isRobbing() {
        return this == ROBBING;
    }

    /**
     * 是否是出牌阶段
     */
    public boolean isCardPlaying() {
        return this == PLAYING;
    }

    /**
     * 是否可以出牌
     */
    public boolean canPlayCards() {
        return this == PLAYING;
    }

    /**
     * 获取对应的房间状态 - 已废弃
     * <p>房间状态由各业务层自行维护，phase/state 由业务根据上下文填充，
     * 不再通过枚举互转方法隐式决定，避免两个枚举字段互相耦合。
     * @deprecated since 0.2：phase 与 roomState 在 DTO 层并存，由调用方显式设置。
     */
    @Deprecated
    public RoomStateEnum toRoomState() {
        switch (this) {
            case WAITING:
                return RoomStateEnum.WAITING;
            case DEALING:
                return RoomStateEnum.DISTRIBUTING;
            case ROBBING:
                return RoomStateEnum.ROBBING;
            case PLAYING:
                return RoomStateEnum.PLAYING;
            case ENDING:
                return RoomStateEnum.ENDING;
            case CLOSED:
                return RoomStateEnum.CLOSED;
            default:
                return RoomStateEnum.WAITING;
        }
    }

    /**
     * 从房间状态获取游戏阶段 - 已废弃
     * <p>见 {@link #toRoomState()}。
     * @deprecated since 0.2：phase 与 roomState 由调用方显式设置。
     */
    @Deprecated
    public static GamePhaseEnum fromRoomState(RoomStateEnum state) {
        if (state == null) {
            return WAITING;
        }
        switch (state) {
            case DISTRIBUTING:
                return DEALING;
            case ROBBING:
                return ROBBING;
            case PLAYING:
                return PLAYING;
            case ENDING:
                return ENDING;
            case CLOSED:
                return CLOSED;
            case WAITING:
            case READY:
            default:
                return WAITING;
        }
    }
}
