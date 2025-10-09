package com.cong.fishisland.model.dto.item;

import com.cong.fishisland.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 物品实例分页查询请求
 * 用于分页获取玩家背包物品列表，可按类别、装备槽位、稀有度筛选，支持排序
 *
 * @author Shing
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("物品实例分页查询请求")
public class ItemInstanceQueryRequest extends PageRequest implements Serializable {

    /**
     * 物品大类：equipment-装备类、consumable-消耗品、material-材料
     */
    @ApiModelProperty(value = "物品大类（可选）", example = "equipment")
    private String category;

    /**
     * 装备槽位: head-头部, hand-手部, foot-脚部, weapon-武器
     */
    @ApiModelProperty(value = "装备槽位（可选）", example = "weapon")
    private String equipSlot;

    /**
     * 稀有度等级（1-8）
     */
    @ApiModelProperty(value = "稀有度等级（可选）", example = "2")
    private Integer rarity;

    private static final long serialVersionUID = 1L;

}