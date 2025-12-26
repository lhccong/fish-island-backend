package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.BossBattleInfoVO;
import com.cong.fishisland.model.vo.game.BossChallengeRankingVO;
import com.cong.fishisland.model.vo.game.BossVO;

import java.util.List;

/**
 * Boss服务接口
 *
 * @author cong
 */
public interface BossService {

    /**
     * 获取Boss列表
     *
     * @return Boss列表
     */
    List<BossVO> getBossList();

    /**
     * 对战方法（10个回合）
     *
     * @param bossId Boss ID
     * @return 10个回合的对战结果列表
     */
    List<BattleResultVO> battle(Long bossId);

    /**
     * 获取Boss挑战排行榜
     *
     * @param bossId Boss ID
     * @param limit 返回数量限制，默认10
     * @return 排行榜列表
     */
    List<BossChallengeRankingVO> getBossChallengeRanking(Long bossId, Integer limit);

    /**
     * 获取当前缓存中的Boss列表数据（包含实时血量）
     *
     * @return Boss列表（包含从Redis获取的当前血量）
     */
    List<BossVO> getBossListWithCache();

    /**
     * 获取Boss对战信息（包含当前用户的宠物信息和Boss信息）
     *
     * @param bossId Boss ID
     * @return Boss对战信息
     */
    BossBattleInfoVO getBossBattleInfo(Long bossId);
}

