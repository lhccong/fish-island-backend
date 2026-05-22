package com.cong.fishisland.config;

import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 宠物装备锻造配置
 */
@Configuration
@ConfigurationProperties(prefix = "fishisland.pet.forge")
@Data
public class PetForgeProperties {

    /**
     * 数值汇总是否计入项链(5)、翅膀(6)的锻造等级加成与词条属性。
     * 关闭时仅影响战力/装备属性统计，不影响锻造、刷新、升级等玩法。
     */
    private boolean includeNecklaceWingsStats = false;

    /**
     * 该槽位锻造数据是否参与属性数值计算
     */
    public boolean includeForgeStatsForSlot(Integer equipSlot) {
        if (equipSlot == null) {
            return false;
        }
        if (includeNecklaceWingsStats) {
            return true;
        }
        return equipSlot != EquipSlotEnum.NECKLACE.getValue()
                && equipSlot != EquipSlotEnum.WINGS.getValue();
    }
}
