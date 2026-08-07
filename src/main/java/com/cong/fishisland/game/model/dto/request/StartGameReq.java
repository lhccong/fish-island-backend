package com.cong.fishisland.game.model.dto.request;

import lombok.Data;

/**
 * 开始游戏请求
 *
 * @author cong
 */
@Data
public class StartGameReq {

    /**
     * 房间ID
     */
    private String roomId;
}
