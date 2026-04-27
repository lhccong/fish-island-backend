package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattleUserStats;

import java.util.List;

/**
 * 摸鱼大乱斗玩家总体统计服务
 */
public interface FishBattleUserStatsService extends IService<FishBattleUserStats> {

    /**
     * 获取用户统计数据（不存在则自动初始化）
     */
    FishBattleUserStats getOrInitByUserId(Long userId);

    /**
     * 获取排行榜（按胜场降序）
     */
    List<FishBattleUserStats> getLeaderboard(int limit);

    /**
     * 检查用户今日是否达到对局上限
     */
    boolean isDailyLimitReached(Long userId);
}
