package com.cong.fishisland.model.enums.turntable;

import lombok.Getter;

/**
 * 奖品类型枚举
 * @author cong
 */
@Getter
public enum PrizeTypeEnum {

    EQUIPMENT(1, "装备"),
    TITLE(2, "称号");

    private final Integer value;
    private final String text;

    PrizeTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PrizeTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PrizeTypeEnum anEnum : PrizeTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
