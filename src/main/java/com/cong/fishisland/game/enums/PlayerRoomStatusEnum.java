package com.cong.fishisland.game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 玩家房间状态枚举
 * 用于追踪玩家的房间状态
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum PlayerRoomStatusEnum {

    /**
     * 不在任何房间
     */
    NONE("none", "不在任何房间"),

    /**
     * 正常在房间中
     */
    IN_ROOM("in_room", "在房间中"),

    /**
     * 临时离开房间（游戏还在进行）
     */
    TEMP_LEAVE("temp_leave", "临时离开");

    private final String code;
    private final String description;
}
