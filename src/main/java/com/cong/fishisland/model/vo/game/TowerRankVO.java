package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 爬塔排行榜条目
 *
 * @author cong
 */
@Data
public class TowerRankVO implements Serializable {

    @ApiModelProperty("名次")
    private Integer rank;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String userName;

    @ApiModelProperty("用户头像")
    private String userAvatar;

    @ApiModelProperty("历史最高通关层数")
    private Integer maxFloor;

    private static final long serialVersionUID = 1L;
}
