package com.cong.fishisland.service.achievement;

import lombok.RequiredArgsConstructor;

import static com.cong.fishisland.constant.PetForgeConstant.MAX_EQUIP_LEVEL;
import static com.cong.fishisland.service.pet.PetCardPlayRules.MAX_LEVEL;

@RequiredArgsConstructor
enum AchievementRule {
    FARM("farm-level", "岛田丰章", "农场等级", "wheat", 10, 10, 200, Integer.MAX_VALUE),
    CARDS("card-level", "百匣寻星", "抽卡等级", "cards", 2, 1, 200, MAX_LEVEL),
    EQUIPMENT("equipment-level", "百炼铸星", "装备强化", "forge", 1, 1, 100, MAX_EQUIP_LEVEL),
    PET("pet-level", "共赴山海", "宠物等级", "companion", 10, 10, 500, 60);

    final String id;
    final String name;
    final String label;
    final String icon;
    final int firstLevel;
    final int interval;
    final int points;
    final int maxLevel;

    int tiersAt(int level) {
        return level < firstLevel ? 0 : (Math.min(level, maxLevel) - firstLevel) / interval + 1;
    }

    int levelAt(int tier) {
        return firstLevel + (tier - 1) * interval;
    }
}
