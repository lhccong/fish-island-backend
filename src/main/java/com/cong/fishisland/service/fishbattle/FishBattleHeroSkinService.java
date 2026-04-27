package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHeroSkin;

import java.util.List;

/**
 * 摸鱼大乱斗英雄皮肤服务
 */
public interface FishBattleHeroSkinService extends IService<FishBattleHeroSkin> {

    /**
     * 获取指定英雄的所有启用皮肤
     */
    List<FishBattleHeroSkin> listByHeroId(String heroId);
}
