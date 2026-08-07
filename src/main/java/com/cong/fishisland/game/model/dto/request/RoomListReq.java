package com.cong.fishisland.game.model.dto.request;

import com.cong.fishisland.game.enums.GameTypeEnum;
import lombok.Data;

/**
 * 房间列表请求
 *
 * @author cong
 */
@Data
public class RoomListReq {

    /**
     * 游戏类型（可选，null 表示所有类型）
     */
    private GameTypeEnum gameType;
}
