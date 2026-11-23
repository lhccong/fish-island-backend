package com.cong.fishisland.service.impl.farm;

import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.FarmLevelRewardService;
import com.cong.fishisland.service.UserPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 农场等级持续奖励：每达到 5 的倍数等级发放一次性积分，奖励随档位递增。
 * 以 farm_user.lastLevelRewardLevel 记录已领取档位，防重复领取；跨档升级逐档补发。
 */
@Slf4j
@Service
public class FarmLevelRewardServiceImpl implements FarmLevelRewardService {

    @Resource
    private FarmUserMapper farmUserMapper;

    @Resource
    private UserPointsService userPointsService;

    @Override
    public void grantRewards(Long userId, int newLevel) {
        if (userId == null || newLevel <= 0) {
            return;
        }
        FarmUser farmUser = farmUserMapper.selectById(userId);
        if (farmUser == null) {
            return;
        }
        int lastClaimed = farmUser.getLastLevelRewardLevel() != null ? farmUser.getLastLevelRewardLevel() : 0;
        if (newLevel <= lastClaimed) {
            return;
        }
        // 从下一个 5 的倍数档位开始逐档补发
        int next = (lastClaimed / FarmConstants.FARM_LEVEL_REWARD_STEP + 1)
                * FarmConstants.FARM_LEVEL_REWARD_STEP;
        for (int level = next; level <= newLevel; level += FarmConstants.FARM_LEVEL_REWARD_STEP) {
            int reward = FarmConstants.levelRewardFor(level);
            if (reward <= 0) {
                continue;
            }
            userPointsService.updateUsedPoints(userId, -reward,
                    PointsRecordSourceEnum.FARM_LEVEL_REWARD.getValue(),
                    null,
                    "农场等级奖励-达到" + level + "级");
            log.info("农场等级奖励发放 userId={}, level={}, reward={}", userId, level, reward);
        }
        farmUserMapper.updateLastLevelRewardLevel(userId, newLevel);
    }
}
