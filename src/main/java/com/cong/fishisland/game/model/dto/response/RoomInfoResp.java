package com.cong.fishisland.game.model.dto.response;

import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.enums.RoomStateEnum;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 房间信息响应
 *
 * @author cong
 */
@Data
@Builder
public class RoomInfoResp {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 游戏类型
     */
    private GameTypeEnum gameType;

    /**
     * 房间状态
     */
    private RoomStateEnum state;

    /**
     * 房主ID
     */
    private Long ownerId;

    /**
     * 玩家数量
     */
    private Integer playerCount;

    /**
     * 最大玩家数
     */
    private Integer maxPlayers;

    /**
     * 是否需要密码
     */
    private Boolean needPassword;

    /**
     * 玩家列表
     */
    private List<PlayerInfoResp> players;
}
