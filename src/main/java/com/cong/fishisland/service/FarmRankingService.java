package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.RankingDTO;
import com.cong.fishisland.model.entity.farm.FarmRanking;

import java.util.List;

/**
 * 农场排行榜服务
 */
public interface FarmRankingService {

    /**
     * 今日偷菜经验排行榜。
     *
     * @return 排行列表
     */
    List<RankingDTO> getTodayStealExpRanking();

    /**
     * 今日偷菜次数排行榜。
     *
     * @return 排行列表
     */
    List<RankingDTO> getTodayStealCountRanking();

    /**
     * 今日防御（被偷）排行榜。
     *
     * @return 排行列表
     */
    List<RankingDTO> getTodayDefenseRanking();

    /**
     * 累计偷菜经验排行榜。
     *
     * @return 排行列表
     */
    List<RankingDTO> getTotalStealExpRanking();

    /**
     * 累计偷菜次数排行榜。
     *
     * @return 排行列表
     */
    List<RankingDTO> getTotalStealCountRanking();

    /**
     * 累计防御（被偷）排行榜。
     *
     * @return 排行列表
     */
    List<RankingDTO> getTotalDefenseRanking();

    /**
     * 偷菜成功后更新今日/累计偷菜次数排行。
     *
     * @param stealerId 偷菜者的农场用户 ID
     */
    void updateStealCountRanking(Long stealerId);

    /**
     * 被偷后更新今日/累计防御排行。
     *
     * @param ownerId 农场主的农场用户 ID
     * @param damage  本次被偷损失的积分（用于排行分值）
     */
    void updateDefenseRanking(Long ownerId, int damage);
}
