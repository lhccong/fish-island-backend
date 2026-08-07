package com.cong.fishisland.game.landlords.dto.request;

import lombok.Data;

/**
 * 游戏操作请求基类
 *
 * @author cong
 */
@Data
public class GameActionReq {
    
    /**
     * 房间ID
     */
    private String roomId;
}
