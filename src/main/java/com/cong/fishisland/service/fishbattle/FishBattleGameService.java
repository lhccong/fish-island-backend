package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleGame;

/**
 * 摸鱼大乱斗对局记录服务
 */
public interface FishBattleGameService extends IService<FishBattleGame> {

    /**
     * 根据ID获取对局详情
     */
    FishBattleGame getGameDetail(Long gameId);

    /**
     * 获取总对局数
     */
    long getTotalGameCount();
}
