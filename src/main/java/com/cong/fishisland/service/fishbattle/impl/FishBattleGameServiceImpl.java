package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleGameMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleGame;
import com.cong.fishisland.service.fishbattle.FishBattleGameService;
import org.springframework.stereotype.Service;

/**
 * 摸鱼大乱斗对局记录服务实现
 */
@Service
public class FishBattleGameServiceImpl extends ServiceImpl<FishBattleGameMapper, FishBattleGame>
        implements FishBattleGameService {

    @Override
    public FishBattleGame getGameDetail(Long gameId) {
        return this.getById(gameId);
    }

    @Override
    public long getTotalGameCount() {
        return this.count();
    }
}
