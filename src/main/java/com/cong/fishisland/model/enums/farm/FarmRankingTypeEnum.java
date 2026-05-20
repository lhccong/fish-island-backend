package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 农场排行类型
 */
@Getter
public enum FarmRankingTypeEnum {

    STEAL_EXP("steal_exp"),
    STEAL_COUNT("steal_count"),
    DEFENSE("defense");

    private final String value;

    FarmRankingTypeEnum(String value) {
        this.value = value;
    }

    public static FarmRankingTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FarmRankingTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
