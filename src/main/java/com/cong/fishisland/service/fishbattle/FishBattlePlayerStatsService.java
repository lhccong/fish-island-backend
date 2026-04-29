package com.cong.fishisland.service.fishbattle;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fishbattle.FishBattlePlayerStats;

import java.util.List;

/**
 * 摸鱼大乱斗玩家对局统计服务
 */
public interface FishBattlePlayerStatsService extends IService<FishBattlePlayerStats> {

    /**
     * 获取某局对局的所有玩家统计
     */
    List<FishBattlePlayerStats> listByGameId(Long gameId);

    /**
     * 获取用户的对局历史（分页）
     */
    IPage<FishBattlePlayerStats> pageByUserId(Long userId, int current, int pageSize);

    /**
     * 点赞
     */
    boolean likePlayer(Long gameId, Long targetUserId, Long currentUserId);
}
