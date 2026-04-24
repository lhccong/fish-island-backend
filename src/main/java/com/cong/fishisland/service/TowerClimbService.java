package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.game.TowerClimbResultVO;
import com.cong.fishisland.model.vo.game.TowerFloorMonsterVO;
import com.cong.fishisland.model.vo.game.TowerProgressVO;
import com.cong.fishisland.model.vo.game.TowerRankVO;

import java.util.List;

/**
 * 爬塔服务接口
 *
 * @author cong
 */
public interface TowerClimbService {

    /**
     * 获取当前用户爬塔进度及下一层怪物信息
     */
    TowerProgressVO getProgress();

    /**
     * 获取指定层的怪物信息
     *
     * @param floor 层数（从1开始，无上限）
     */
    TowerFloorMonsterVO getFloorMonster(int floor);

    /**
     * 挑战当前层（maxFloor + 1）
     *
     * @return 战斗结果
     */
    TowerClimbResultVO challenge();

    /**
     * 获取爬塔排行榜（按最高通关层数降序）
     *
     * @param limit 返回条数，默认100
     */
    List<TowerRankVO> getRanking(int limit);
}
