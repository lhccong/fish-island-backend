package com.cong.fishisland.model.dto.game;

import lombok.Data;

/**
 * 加入谁是卧底游戏房间请求
 *
 * @author cong
 */
@Data
public class UndercoverRoomJoinRequest {

    /**
     * 房间ID
     */
    private String roomId;
} 