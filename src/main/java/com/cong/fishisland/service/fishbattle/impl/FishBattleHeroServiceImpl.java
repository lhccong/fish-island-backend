package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleHeroMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHero;
import com.cong.fishisland.service.fishbattle.FishBattleHeroService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 摸鱼大乱斗英雄服务实现
 */
@Service
public class FishBattleHeroServiceImpl extends ServiceImpl<FishBattleHeroMapper, FishBattleHero>
        implements FishBattleHeroService {

    @Override
    public List<FishBattleHero> listEnabledHeroes() {
        return this.list(new LambdaQueryWrapper<FishBattleHero>()
                .eq(FishBattleHero::getStatus, 1)
                .orderByAsc(FishBattleHero::getId));
    }

    @Override
    public FishBattleHero getByHeroId(String heroId) {
        return this.getOne(new LambdaQueryWrapper<FishBattleHero>()
                .eq(FishBattleHero::getHeroId, heroId));
    }
}
