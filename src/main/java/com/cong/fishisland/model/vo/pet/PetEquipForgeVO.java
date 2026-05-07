package com.cong.fishisland.model.vo.pet;

import com.cong.fishisland.model.entity.pet.EquipEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 宠物装备锻造 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "PetEquipForgeVO", description = "宠物装备锻造信息")
public class PetEquipForgeVO implements Serializable {

    @ApiModelProperty(value = "记录ID")
    private Long id;

    @ApiModelProperty(value = "宠物ID")
    private Long petId;

    /** 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀 */
    @ApiModelProperty(value = "装备位置：1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀")
    private Integer equipSlot;

    /** 装备位置名称 */
    @ApiModelProperty(value = "装备位置名称，如：武器、手套、鞋子等")
    private String equipSlotName;

    /** 装备等级（武器为 null） */
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

    private static final long serialVersionUID = 1L;
}
