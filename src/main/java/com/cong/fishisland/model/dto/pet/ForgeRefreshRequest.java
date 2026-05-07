package com.cong.fishisland.model.dto.pet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 刷新装备词条请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "ForgeRefreshRequest", description = "刷新装备词条请求，基础消耗 100 积分，每锁定一条额外 +50 积分")
public class ForgeRefreshRequest implements Serializable {

    /**
     * 宠物ID
     */
    @ApiModelProperty(value = "宠物ID", required = true, example = "1001")
    private Long petId;

    /**
     * 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     */
    @ApiModelProperty(value = "装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀", required = true, example = "1")
    private Integer equipSlot;

    private static final long serialVersionUID = 1L;
}
