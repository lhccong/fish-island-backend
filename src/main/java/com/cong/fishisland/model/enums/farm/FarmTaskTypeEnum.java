package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 农场每日任务类型
 */
@Getter
public enum FarmTaskTypeEnum {

    HARVEST("harvest"),
    REPLANT("replant"),
    PLANT("plant"),
    VISIT("visit"),
    STEAL("steal");

    private final String value;

    FarmTaskTypeEnum(String value) {
        this.value = value;
    }

    public static FarmTaskTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FarmTaskTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
