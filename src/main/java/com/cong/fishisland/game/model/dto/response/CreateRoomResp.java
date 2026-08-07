package com.cong.fishisland.game.model.dto.response;

import com.cong.fishisland.game.enums.GameTypeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 创建房间响应
 *
 * @author cong
 */
@Data
@Builder
public class CreateRoomResp {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 游戏类型
     */
    private GameTypeEnum gameType;

    /**
     * 房间信息
     */
    private RoomInfoResp roomInfo;
}
