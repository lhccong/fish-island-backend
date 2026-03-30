package com.cong.fishisland.service.turntable.strategy.impl;

import com.cong.fishisland.model.entity.turntable.TurntablePrize;
import com.cong.fishisland.service.turntable.strategy.DrawStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 权重随机抽奖策略
 * 根据奖品的概率权重进行随机抽取
 * @author cong
 */
@Component
public class WeightRandomDrawStrategy implements DrawStrategy {

    private final Random random = new Random();

    @Override
    public TurntablePrize draw(List<TurntablePrize> prizes) {
        if (prizes == null || prizes.isEmpty()) {
            return null;
        }

        // 计算总权重
        int totalWeight = prizes.stream()
                .mapToInt(p -> p.getProbability() != null ? p.getProbability() : 0)
                .sum();

        if (totalWeight <= 0) {
            return null;
        }

        // 生成随机数 [0, totalWeight)
        int randomValue = random.nextInt(totalWeight);

        // 根据权重选择奖品
        int currentWeight = 0;
        for (TurntablePrize prize : prizes) {
            int weight = prize.getProbability() != null ? prize.getProbability() : 0;
            currentWeight += weight;
            if (randomValue < currentWeight) {
                return prize;
            }
        }

        // 理论上不会走到这里，但返回最后一个作为兜底
        return prizes.get(prizes.size() - 1);
    }

    @Override
    public String getStrategyName() {
        return "WEIGHT_RANDOM";
    }
}
