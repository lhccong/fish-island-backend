package com.cong.fishisland.model.enums.pet;

import lombok.Getter;

/**
 * 宠物装备位置枚举
 * 武器不需要等级字段，其余位置等级继承宠物等级
 *
 * @author cong
 */
@Getter
public enum EquipSlotEnum {

    WEAPON(1, "武器"),
    GLOVES(2, "手套"),
    SHOES(3, "鞋子"),
    HELMET(4, "头盔"),
    NECKLACE(5, "项链"),
    WINGS(6, "翅膀");

    private final int value;
    private final String label;

    EquipSlotEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public static EquipSlotEnum of(int value) {
        for (EquipSlotEnum e : values()) {
            if (e.value == value) return e;
        }
        throw new IllegalArgumentException("未知装备位置: " + value);
    }
}
