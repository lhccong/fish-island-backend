package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleSummonerSpell;

import java.util.List;

/**
 * 摸鱼大乱斗召唤师技能服务
 */
public interface FishBattleSummonerSpellService extends IService<FishBattleSummonerSpell> {

    /**
     * 获取所有启用的召唤师技能
     */
    List<FishBattleSummonerSpell> listEnabledSpells();
}
