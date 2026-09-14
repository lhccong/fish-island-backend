package com.cong.fishisland.service;

/**
 * 农场等级持续奖励服务：每升 5 级发放一次性积分奖励（档位越高奖励越多）。
 */
public interface FarmLevelRewardService {

    /**
     * 升级后补发 (lastLevelRewardLevel, newLevel] 区间内所有 5 的倍数档位奖励。
     * 由 {@code FarmUserServiceImpl#syncLevelIfNeeded} 在等级提升时调用。
     */
    void grantRewards(Long userId, int newLevel);
}
