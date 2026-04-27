package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleHeroSkinMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHeroSkin;
import com.cong.fishisland.service.fishbattle.FishBattleHeroSkinService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 摸鱼大乱斗英雄皮肤服务实现
 */
@Service
public class FishBattleHeroSkinServiceImpl extends ServiceImpl<FishBattleHeroSkinMapper, FishBattleHeroSkin>
        implements FishBattleHeroSkinService {

    @Override
    public List<FishBattleHeroSkin> listByHeroId(String heroId) {
        return this.list(new LambdaQueryWrapper<FishBattleHeroSkin>()
                .eq(FishBattleHeroSkin::getHeroId, heroId)
                .eq(FishBattleHeroSkin::getStatus, 1)
                .orderByDesc(FishBattleHeroSkin::getIsDefault)
                .orderByAsc(FishBattleHeroSkin::getId));
    }
}
