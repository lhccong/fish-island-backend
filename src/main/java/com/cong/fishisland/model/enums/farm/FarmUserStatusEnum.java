package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 农场用户状态：0-禁用，1-正常
 */
@Getter
public enum FarmUserStatusEnum {

    DISABLED(0),
    NORMAL(1);

    private final int value;

    FarmUserStatusEnum(int value) {
        this.value = value;
    }
}
