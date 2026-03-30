package com.cong.fishisland.service.turntable.strategy.impl;

import com.cong.fishisland.model.entity.turntable.TurntablePrize;
import com.cong.fishisland.service.turntable.strategy.DrawStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 保底抽奖策略
 * 当触发保底时，从指定品质及以上的奖品中抽取
 * @author cong
 */
@Component
public class GuaranteeDrawStrategy implements DrawStrategy {

    private final Random random = new Random();

    /**
     * 保底品质阈值
     * 小保底：抽取稀有(R)及以上品质，即 quality >= 2
     * 大保底：抽取史诗(SR)及以上品质，即 quality >= 3
     */
    private int minQuality = 2;

    @Override
    public TurntablePrize draw(List<TurntablePrize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            return null;
        }

        // 过滤出符合保底品质的奖品
        List<TurntablePrize> qualifiedPrizes = prizes.stream()
                .filter(p -> p.getQuality() != null && p.getQuality() >= minQuality)
                .collect(Collectors.toList());

        if (qualifiedPrizes.isEmpty()) {
            // 如果没有符合品质的奖品，降级使用全部奖品
            qualifiedPrizes = prizes;
        }

        // 计算总权重
        int totalWeight = qualifiedPrizes.stream()
                .mapToInt(p -> p.getProbability() != null ? p.getProbability() : 0)
                .sum();

        if (totalWeight <= 0) {
            return qualifiedPrizes.get(random.nextInt(qualifiedPrizes.size()));
        }

        // 根据权重选择奖品
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (TurntablePrize prize : qualifiedPrizes) {
            int weight = prize.getProbability() != null ? prize.getProbability() : 0;
            currentWeight += weight;
            if (randomValue < currentWeight) {
                return prize;
            }
        }

        return qualifiedPrizes.get(qualifiedPrizes.size() - 1);
    }

    @Override
    public String getStrategyName() {
        return "GUARANTEE";
    }

    /**
     * 设置保底品质阈值
     * @param minQuality 最小品质
     */
    public void setMinQuality(int minQuality) {
        this.minQuality = minQuality;
    }

    /**
     * 获取保底品质阈值
     * @return 最小品质
     */
    public int getMinQuality() {
        return minQuality;
    }
}
