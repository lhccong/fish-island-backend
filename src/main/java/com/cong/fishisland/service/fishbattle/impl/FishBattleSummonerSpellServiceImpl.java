package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleSummonerSpellMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleSummonerSpell;
import com.cong.fishisland.service.fishbattle.FishBattleSummonerSpellService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 摸鱼大乱斗召唤师技能服务实现
 */
@Service
public class FishBattleSummonerSpellServiceImpl extends ServiceImpl<FishBattleSummonerSpellMapper, FishBattleSummonerSpell>
        implements FishBattleSummonerSpellService {

    @Override
    public List<FishBattleSummonerSpell> listEnabledSpells() {
        return this.list(new LambdaQueryWrapper<FishBattleSummonerSpell>()
                .eq(FishBattleSummonerSpell::getStatus, 1)
                .orderByAsc(FishBattleSummonerSpell::getId));
    }
}
