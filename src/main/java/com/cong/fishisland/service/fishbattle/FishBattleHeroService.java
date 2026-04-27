package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHero;

import java.util.List;

/**
 * 摸鱼大乱斗英雄服务
 */
public interface FishBattleHeroService extends IService<FishBattleHero> {

    /**
     * 获取所有启用的英雄列表
     */
    List<FishBattleHero> listEnabledHeroes();

    /**
     * 根据英雄标识获取英雄
     */
    FishBattleHero getByHeroId(String heroId);
}
