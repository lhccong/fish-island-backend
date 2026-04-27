package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattlePlayerStatsMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattlePlayerStats;
import com.cong.fishisland.service.fishbattle.FishBattlePlayerStatsService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 摸鱼大乱斗玩家对局统计服务实现
 */
@Service
public class FishBattlePlayerStatsServiceImpl extends ServiceImpl<FishBattlePlayerStatsMapper, FishBattlePlayerStats>
        implements FishBattlePlayerStatsService {

    @Override
    public List<FishBattlePlayerStats> listByGameId(Long gameId) {
        return this.list(new LambdaQueryWrapper<FishBattlePlayerStats>()
                .eq(FishBattlePlayerStats::getGameId, gameId)
                .orderByAsc(FishBattlePlayerStats::getTeam));
    }

    @Override
    public IPage<FishBattlePlayerStats> pageByUserId(Long userId, int current, int pageSize) {
        return this.page(new Page<>(current, pageSize),
                new LambdaQueryWrapper<FishBattlePlayerStats>()
                        .eq(FishBattlePlayerStats::getUserId, userId)
                        .orderByDesc(FishBattlePlayerStats::getCreateTime));
    }

    @Override
    public boolean likePlayer(Long gameId, Long targetUserId) {
        FishBattlePlayerStats stats = this.getOne(new LambdaQueryWrapper<FishBattlePlayerStats>()
                .eq(FishBattlePlayerStats::getGameId, gameId)
                .eq(FishBattlePlayerStats::getUserId, targetUserId));
        if (stats == null) {
            return false;
        }
        stats.setLikes(stats.getLikes() + 1);
        return this.updateById(stats);
    }
}
