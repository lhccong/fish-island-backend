package com.cong.fishisland.game.model.dto.response;

import com.cong.fishisland.game.landlords.dto.response.GameStateResp;
import lombok.Builder;
import lombok.Data;

/**
 * 加入房间响应
 *
 * @author cong
 */
@Data
@Builder
public class JoinRoomResp {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 玩家ID
     */
    private Long playerId;

    /**
     * 当前房间人数
     */
    private Integer playerCount;

    /**
     * 房间信息
     */
    private RoomInfoResp roomInfo;

    /**
     * 是否是重连
     */
    private Boolean reconnect;

    /**
     * 重连时的游戏状态（仅重连时返回）
     */
    private GameStateResp gameState;
}
