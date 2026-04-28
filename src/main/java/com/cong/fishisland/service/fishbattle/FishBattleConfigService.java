package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleConfig;

/**
 * 摸鱼大乱斗游戏配置 Service
 */
public interface FishBattleConfigService extends IService<FishBattleConfig> {

    /**
     * 根据 configKey 获取启用状态的配置数据（JSON字符串）
     *
     * @param configKey 配置标识
     * @return configData JSON字符串，未找到返回 null
     */
    String getConfigData(String configKey);
}
