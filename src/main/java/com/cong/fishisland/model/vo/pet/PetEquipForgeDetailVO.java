package com.cong.fishisland.model.vo.pet;

import com.cong.fishisland.model.entity.pet.EquipEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 宠物单件装备锻造详情 VO（含升级消耗信息）
 *
 * @author cong
 */
@Data
@ApiModel(value = "PetEquipForgeDetailVO", description = "宠物单件装备锻造详情，包含词条属性及本次升级所需积分")
public class PetEquipForgeDetailVO implements Serializable {

    @ApiModelProperty(value = "记录ID")
    private Long id;

    @ApiModelProperty(value = "宠物ID")
    private Long petId;

    @ApiModelProperty(value = "装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀")
    private Integer equipSlot;

    @ApiModelProperty(value = "装备位置名称，如：武器、手套、鞋子等")
    private String equipSlotName;

    @ApiModelProperty(value = "装备等级，武器为 null")
    private Integer equipLevel;

    @ApiModelProperty(value = "词条1")
    private EquipEntry entry1;

    @ApiModelProperty(value = "词条2")
    private EquipEntry entry2;

    @ApiModelProperty(value = "词条3")
    private EquipEntry entry3;

    @ApiModelProperty(value = "词条4")
    private EquipEntry entry4;

    @ApiModelProperty(value = "本次升级所需积分（已达最高等级时为 0）", example = "90")
    private Integer nextUpgradeCost;

    @ApiModelProperty(value = "本次升级成功概率（百分比，已达最高等级时为 0）", example = "55")
    private Integer successRate;

    @ApiModelProperty(value = "是否已达最高等级", example = "false")
    private Boolean maxLevel;

    private static final long serialVersionUID = 1L;
}
