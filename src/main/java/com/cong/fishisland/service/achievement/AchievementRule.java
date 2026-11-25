package com.cong.fishisland.service.achievement;

import com.cong.fishisland.service.pet.PetCardPlayRules;
import com.cong.fishisland.service.user.UserLevelRules;
import lombok.RequiredArgsConstructor;

import static com.cong.fishisland.constant.PetForgeConstant.MAX_EQUIP_LEVEL;

@RequiredArgsConstructor
enum AchievementRule {
    USER("user-level", "用户成长", "用户等级", "user", "级", 2, 1, 200, UserLevelRules.MAX_LEVEL),
    FARM("farm-level", "岛田丰章", "农场等级", "wheat", "级", 10, 10, 200, Integer.MAX_VALUE),
    CARDS("card-level", "百匣寻星", "抽卡等级", "cards", "级", 2, 1, 200, PetCardPlayRules.MAX_LEVEL),
    EQUIPMENT("equipment-level", "百炼铸星", "装备强化", "forge", "级", 1, 1, 100, MAX_EQUIP_LEVEL),
    PET("pet-level", "共赴山海", "宠物等级", "companion", "级", 10, 10, 500, 60),
    TURNTABLE("turntable-draw-count", "转盘抽奖", "累计抽奖", "turntable", "次", 100, 100, 500, 300);

    final String id;
    final String name;
    final String label;
    final String icon;
    final String unit;
    final int firstMilestone;
    final int interval;
    final int points;
    final int maxMilestone;

    int tiersAt(int progress) {
        return progress < firstMilestone ? 0 : (Math.min(progress, maxMilestone) - firstMilestone) / interval + 1;
    }

    int milestoneAt(int tier) {
        return firstMilestone + (tier - 1) * interval;
    }
}
