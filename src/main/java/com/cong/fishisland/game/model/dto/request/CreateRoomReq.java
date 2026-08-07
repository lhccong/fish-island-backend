package com.cong.fishisland.game.model.dto.request;

import com.cong.fishisland.game.enums.GameTypeEnum;
import lombok.Data;

/**
 * 创建房间请求
 *
 * @author cong
 */
@Data
public class CreateRoomReq {

    /**
     * 游戏类型
     */
    private GameTypeEnum gameType;

    /**
     * 房间密码（可选）
     */
    private String password;
}
