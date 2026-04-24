package com.cong.fishisland.model.vo.pet;

import com.cong.fishisland.model.entity.pet.EquipEntry;
import lombok.Data;

import java.io.Serializable;

/**
 * 宠物装备锻造 VO
 *
 * @author cong
 */
@Data
public class PetEquipForgeVO implements Serializable {

    private Long id;

    private Long petId;

    /** 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀 */
    private Integer equipSlot;

    /** 装备位置名称 */
    private String equipSlotName;

    /** 装备等级（武器为 null） */
    private Integer equipLevel;

    private EquipEntry entry1;
    private EquipEntry entry2;
    private EquipEntry entry3;
    private EquipEntry entry4;

    private static final long serialVersionUID = 1L;
}
