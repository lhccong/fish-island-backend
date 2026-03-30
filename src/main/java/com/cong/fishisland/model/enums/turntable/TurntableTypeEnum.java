package com.cong.fishisland.model.enums.turntable;

import lombok.Getter;

/**
 * 转盘类型枚举
 * @author cong
 */
@Getter
public enum TurntableTypeEnum {

    EQUIPMENT(1, "宠物装备转盘"),
    TITLE(2, "称号转盘");

    private final Integer value;
    private final String text;

    TurntableTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static TurntableTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (TurntableTypeEnum anEnum : TurntableTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
