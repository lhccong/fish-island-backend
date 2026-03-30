package com.cong.fishisland.service.turntable.strategy;

import com.cong.fishisland.model.entity.turntable.TurntablePrize;

import java.util.List;

/**
 * 抽奖策略接口
 * @author cong
 */
public interface DrawStrategy {
    /**
     * 根据策略抽取奖品
     * @param prizes 奖品列表
     * @return 抽中的奖品
     */
    TurntablePrize draw(List<TurntablePrize> prizes);

    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
}
