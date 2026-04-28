package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleConfigMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleConfig;
import com.cong.fishisland.service.fishbattle.FishBattleConfigService;
import org.springframework.stereotype.Service;

/**
 * 摸鱼大乱斗游戏配置 Service 实现
 */
@Service
public class FishBattleConfigServiceImpl extends ServiceImpl<FishBattleConfigMapper, FishBattleConfig>
        implements FishBattleConfigService {

    @Override
    public String getConfigData(String configKey) {
        FishBattleConfig config = this.getOne(
                new LambdaQueryWrapper<FishBattleConfig>()
                        .eq(FishBattleConfig::getConfigKey, configKey)
                        .eq(FishBattleConfig::getStatus, 1)
        );
        return config != null ? config.getConfigData() : null;
    }
}
