package com.cong.fishisland.model.enums.turntable;

import lombok.Getter;

/**
 * 奖励品质枚举
 * @author cong
 */
@Getter
public enum PrizeQualityEnum {

    NORMAL(1, "普通(N)"),
    RARE(2, "稀有(R)"),
    EPIC(3, "史诗(SR)"),
    LEGENDARY(4, "传说(SSR)");

    private final Integer value;
    private final String text;

    PrizeQualityEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     */
    public static PrizeQualityEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (PrizeQualityEnum anEnum : PrizeQualityEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
