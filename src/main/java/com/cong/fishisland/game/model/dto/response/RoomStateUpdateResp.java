package com.cong.fishisland.game.model.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 房间状态更新响应
 */
@Data
@Builder
public class RoomStateUpdateResp {

    /**
     * 事件类型
     * @see com.cong.fishisland.game.enums.GameActionEnum
     */
    private String event;

    /**
     * 玩家信息
     */
    private PlayerInfoResp player;

    /**
     * 玩家名称（离开事件时使用）
     */
    private String playerName;

    /**
     * 当前房间人数
     */
    private Integer playerCount;

    /**
     * 完整房间信息（包含所有玩家列表）
     */
    private RoomInfoResp roomInfo;
}
