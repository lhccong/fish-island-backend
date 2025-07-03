package com.cong.fishisland.model.dto.game;

import lombok.Data;

/**
 * 退出谁是卧底游戏房间请求
 *
 * @author cong
 */
@Data
public class UndercoverRoomQuitRequest {

    /**
     * 房间ID
     */
    private String roomId;
} 