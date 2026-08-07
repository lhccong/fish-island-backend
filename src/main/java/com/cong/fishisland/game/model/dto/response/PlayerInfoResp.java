package com.cong.fishisland.game.model.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 玩家信息响应
 *
 * @author cong
 */
@Data
@Builder
public class PlayerInfoResp {

    /**
     * 玩家ID
     */
    private Long userId;

    /**
     * 玩家名称
     */
    private String userName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 是否准备
     */
    private Boolean ready;

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 是否是地主
     */
    private Boolean isLandlord;

    /**
     * 角色 (房主/玩家/观战)
     */
    private String role;

    /**
     * 叫分 (0表示不叫)
     */
    private Integer robScore;

    /**
     * 牌数
     */
    private Integer cardCount;

    /**
     * 是否被AI托管
     */
    private Boolean robotControlled;
}
