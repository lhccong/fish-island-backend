package com.cong.fishisland.model.dto.pet;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 装备升级请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "ForgeUpgradeRequest", description = "装备升级请求，消耗积分随等级递增，成功概率随等级递减")
public class ForgeUpgradeRequest implements Serializable {

    /**
     * 宠物ID
     */
    @ApiModelProperty(value = "宠物ID", required = true, example = "1001")
    private Long petId;

    /**
     * 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     *
     */
    @ApiModelProperty(value = "装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀", required = true, example = "1")
    private Integer equipSlot;

    private static final long serialVersionUID = 1L;
}
