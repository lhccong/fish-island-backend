package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

import java.time.LocalDateTime;

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

    /**
     * 根据当前时间解析返回给前端的地块状态：种植中且已到达收获时间则视为已成熟（可收获）。
     */
    public static Integer resolveDisplayStatus(Integer status, LocalDateTime harvestTime, LocalDateTime now) {
        if (!isPlanted(status)) {
            return status;
        }
        if (Integer.valueOf(MATURE.getValue()).equals(status)) {
            return status;
        }
        if (harvestTime != null && !harvestTime.isAfter(now)) {
            return MATURE.getValue();
        }
        return status;
    }
}
