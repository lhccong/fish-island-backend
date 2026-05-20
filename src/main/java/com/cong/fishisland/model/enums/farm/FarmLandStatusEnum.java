package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 地块状态：0-空闲，1-种植中，2-已成熟
 */
@Getter
public enum FarmLandStatusEnum {

    IDLE(0),
    PLANTING(1),
    MATURE(2);

    private final int value;

    FarmLandStatusEnum(int value) {
        this.value = value;
    }

    public static FarmLandStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (FarmLandStatusEnum status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public static boolean isPlanted(Integer status) {
        return status != null && status >= PLANTING.value;
    }
}
