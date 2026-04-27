package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattleUserStatsMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattleUserStats;
import com.cong.fishisland.service.fishbattle.FishBattleUserStatsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

/**
 * 摸鱼大乱斗玩家总体统计服务实现
 */
@Service
public class FishBattleUserStatsServiceImpl extends ServiceImpl<FishBattleUserStatsMapper, FishBattleUserStats>
        implements FishBattleUserStatsService {

    @Override
    public FishBattleUserStats getOrInitByUserId(Long userId) {
        FishBattleUserStats stats = this.getOne(new LambdaQueryWrapper<FishBattleUserStats>()
                .eq(FishBattleUserStats::getUserId, userId));
        if (stats == null) {
            stats = new FishBattleUserStats();
            stats.setUserId(userId);
            stats.setTotalGames(0);
            stats.setWins(0);
            stats.setLosses(0);
            stats.setTotalKills(0);
            stats.setTotalDeaths(0);
            stats.setTotalAssists(0);
            stats.setMvpCount(0);
            stats.setCurrentStreak(0);
            stats.setMaxStreak(0);
            stats.setTodayGames(0);
            stats.setTodayDate(Date.valueOf(LocalDate.now()));
            stats.setDailyLimit(20);
            this.save(stats);
        }
        return stats;
    }

    @Override
    public List<FishBattleUserStats> getLeaderboard(int limit) {
        return this.list(new LambdaQueryWrapper<FishBattleUserStats>()
                .orderByDesc(FishBattleUserStats::getWins)
                .last("LIMIT " + limit));
    }

    @Override
    public boolean isDailyLimitReached(Long userId) {
        FishBattleUserStats stats = getOrInitByUserId(userId);
        LocalDate today = LocalDate.now();
        // 如果日期不一致，重置今日计数
        if (stats.getTodayDate() == null || !new java.sql.Date(stats.getTodayDate().getTime()).toLocalDate().equals(today)) {
            stats.setTodayGames(0);
            stats.setTodayDate(Date.valueOf(today));
            this.updateById(stats);
        }
        return stats.getTodayGames() >= stats.getDailyLimit();
    }
}
