package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 农场通用 0/1 标志（未/是）
 */
@Getter
public enum FarmYesNoEnum {

    NO(0),
    YES(1);

    private final int value;

    FarmYesNoEnum(int value) {
        this.value = value;
    }

    public static boolean isYes(Integer value) {
        return value != null && value == YES.value;
    }

    public static boolean isNo(Integer value) {
        return value == null || value == NO.value;
    }
}
