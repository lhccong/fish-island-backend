package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 农场成就任务类型
 */
@Getter
public enum FarmAchievementTypeEnum {

    /** buff 升级到指定等级：targetId = buffType，targetCount = 目标等级 */
    BUFF_UPGRADE("buff_upgrade"),
    /** 购买指定 buff：targetId = buffType */
    BUFF_PURCHASE("buff_purchase"),
    /** 解锁指定品级的图鉴：targetCount = 品级下限 */
    COLLECTION_UNLOCK("collection_unlock"),
    /** 农场等级达到指定值：targetCount = 等级 */
    FARM_LEVEL("farm_level");

    private final String value;

    FarmAchievementTypeEnum(String value) {
        this.value = value;
    }

    public static FarmAchievementTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FarmAchievementTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
