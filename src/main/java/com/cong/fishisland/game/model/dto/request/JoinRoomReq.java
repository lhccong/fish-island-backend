package com.cong.fishisland.game.model.dto.request;

import lombok.Data;

/**
 * 加入房间请求
 *
 * @author cong
 */
@Data
public class JoinRoomReq {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 房间密码（如果有）
     */
    private String password;
}
