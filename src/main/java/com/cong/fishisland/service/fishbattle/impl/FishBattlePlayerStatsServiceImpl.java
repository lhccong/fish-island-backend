package com.cong.fishisland.service.fishbattle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.fishbattle.FishBattlePlayerStatsMapper;
import com.cong.fishisland.model.entity.fishbattle.FishBattlePlayerStats;
import com.cong.fishisland.service.fishbattle.FishBattlePlayerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 摸鱼大乱斗玩家对局统计服务实现
 */
@Service
@RequiredArgsConstructor
public class FishBattlePlayerStatsServiceImpl extends ServiceImpl<FishBattlePlayerStatsMapper, FishBattlePlayerStats>
        implements FishBattlePlayerStatsService {

    private final StringRedisTemplate stringRedisTemplate;

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
    public boolean likePlayer(Long gameId, Long targetUserId, Long currentUserId) {
        // Redis 防重复：同一用户对同一局同一目标只能点赞一次
        String redisKey = "fishBattle:like:" + gameId + ":" + targetUserId + ":" + currentUserId;
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 7, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(absent)) {
            return false;
        }
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
