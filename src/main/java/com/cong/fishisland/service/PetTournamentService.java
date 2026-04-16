package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.game.TournamentChallengeResultVO;
import com.cong.fishisland.model.vo.game.TournamentRankVO;

import java.util.List;

/**
 * 宠物武道大会服务接口
 *
 * @author cong
 */
public interface PetTournamentService {

    /**
     * 挑战指定位数
     * 规则：当前用户无排名，或排名比目标位数低（数字更大），才可挑战
     * 目标坑位有人则与其对战，无人则直接占坑
     *
     * @param targetRank 目标名次（从1开始）
     * @return 挑战结果
     */
    TournamentChallengeResultVO challenge(int targetRank);

    /**
     * 获取当日排行榜
     *
     * @return 排行榜列表（按名次升序）
     */
    List<TournamentRankVO> getLeaderboard();

    /**
     * 获取当前用户的排名（无排名返回null）
     *
     * @return 名次
     */
    Integer getMyRank();

    /**
     * 每日重置排行榜（定时任务调用）
     */
    void resetDailyLeaderboard();
}
