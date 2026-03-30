package com.cong.fishisland.service.turntable.strategy;

import com.cong.fishisland.model.entity.turntable.TurntablePrize;
import com.cong.fishisland.service.turntable.strategy.impl.GuaranteeDrawStrategy;
import com.cong.fishisland.service.turntable.strategy.impl.WeightRandomDrawStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抽奖策略测试
 * @author cong
 */
class DrawStrategyTest {

    private WeightRandomDrawStrategy weightRandomDrawStrategy;
    private GuaranteeDrawStrategy guaranteeDrawStrategy;
    private List<TurntablePrize> testPrizes;

    @BeforeEach
    void setUp() {
        weightRandomDrawStrategy = new WeightRandomDrawStrategy();
        guaranteeDrawStrategy = new GuaranteeDrawStrategy();
        testPrizes = createTestPrizes();
    }

    /**
     * 测试权重随机策略 - 正常抽取
     */
    @Test
    void testWeightRandomDraw_Success() {
        // 执行多次抽奖，验证概率分布
        Map<Integer, Integer> qualityCount = new HashMap<>();
        int drawTimes = 10000;

        for (int i = 0; i < drawTimes; i++) {
            TurntablePrize prize = weightRandomDrawStrategy.draw(testPrizes);
            assertNotNull(prize);
            qualityCount.merge(prize.getQuality(), 1, Integer::sum);
        }

        // 验证各品质都有抽中
        assertTrue(qualityCount.containsKey(1)); // 普通
        assertTrue(qualityCount.containsKey(2)); // 稀有
        assertTrue(qualityCount.containsKey(3)); // 史诗
        assertTrue(qualityCount.containsKey(4)); // 传说

        // 验证概率大致符合权重（普通应该最多）
        assertTrue(qualityCount.get(1) > qualityCount.get(4));
    }

    /**
     * 测试权重随机策略 - 空列表
     */
    @Test
    void testWeightRandomDraw_EmptyList() {
        TurntablePrize prize = weightRandomDrawStrategy.draw(new ArrayList<>());
        assertNull(prize);
    }

    /**
     * 测试权重随机策略 - null列表
     */
    @Test
    void testWeightRandomDraw_NullList() {
        TurntablePrize prize = weightRandomDrawStrategy.draw(null);
        assertNull(prize);
    }

    /**
     * 测试权重随机策略 - 单个奖品
     */
    @Test
    void testWeightRandomDraw_SinglePrize() {
        List<TurntablePrize> singlePrize = new ArrayList<>();
        TurntablePrize prize = new TurntablePrize();
        prize.setId(1L);
        prize.setQuality(1);
        prize.setProbability(1000);
        singlePrize.add(prize);

        TurntablePrize result = weightRandomDrawStrategy.draw(singlePrize);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    /**
     * 测试保底策略 - 小保底（稀有及以上）
     */
    @Test
    void testGuaranteeDraw_SmallGuarantee() {
        guaranteeDrawStrategy.setMinQuality(2); // 稀有及以上

        // 执行多次抽奖，验证只抽中稀有及以上
        for (int i = 0; i < 100; i++) {
            TurntablePrize prize = guaranteeDrawStrategy.draw(testPrizes);
            assertNotNull(prize);
            assertTrue(prize.getQuality() >= 2, "保底应抽中稀有及以上品质");
        }
    }

    /**
     * 测试保底策略 - 大保底（史诗及以上）
     */
    @Test
    void testGuaranteeDraw_BigGuarantee() {
        guaranteeDrawStrategy.setMinQuality(3); // 史诗及以上

        // 执行多次抽奖，验证只抽中史诗及以上
        for (int i = 0; i < 100; i++) {
            TurntablePrize prize = guaranteeDrawStrategy.draw(testPrizes);
            assertNotNull(prize);
            assertTrue(prize.getQuality() >= 3, "大保底应抽中史诗及以上品质");
        }
    }

    /**
     * 测试保底策略 - 空列表
     */
    @Test
    void testGuaranteeDraw_EmptyList() {
        guaranteeDrawStrategy.setMinQuality(2);
        TurntablePrize prize = guaranteeDrawStrategy.draw(new ArrayList<>());
        assertNull(prize);
    }

    /**
     * 测试保底策略 - 无符合品质的奖品时降级
     */
    @Test
    void testGuaranteeDraw_NoQualifiedPrize() {
        guaranteeDrawStrategy.setMinQuality(5); // 不存在的品质

        // 应该降级使用所有奖品
        TurntablePrize prize = guaranteeDrawStrategy.draw(testPrizes);
        assertNotNull(prize);
    }

    /**
     * 测试保底策略 - 只有普通品质奖品
     */
    @Test
    void testGuaranteeDraw_OnlyNormalQuality() {
        List<TurntablePrize> normalPrizes = new ArrayList<>();
        TurntablePrize prize = new TurntablePrize();
        prize.setId(1L);
        prize.setQuality(1); // 普通
        prize.setProbability(1000);
        normalPrizes.add(prize);

        guaranteeDrawStrategy.setMinQuality(2); // 要求稀有及以上

        // 应该降级返回普通品质
        TurntablePrize result = guaranteeDrawStrategy.draw(normalPrizes);
        assertNotNull(result);
        assertEquals(1, result.getQuality());
    }

    /**
     * 测试策略名称
     */
    @Test
    void testStrategyName() {
        assertEquals("WEIGHT_RANDOM", weightRandomDrawStrategy.getStrategyName());
        assertEquals("GUARANTEE", guaranteeDrawStrategy.getStrategyName());
    }

    /**
     * 测试保底策略的minQuality设置
     */
    @Test
    void testGuaranteeStrategy_SetMinQuality() {
        guaranteeDrawStrategy.setMinQuality(4);
        assertEquals(4, guaranteeDrawStrategy.getMinQuality());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试奖品列表
     */
    private List<TurntablePrize> createTestPrizes() {
        List<TurntablePrize> prizes = new ArrayList<>();

        // 普通品质奖品 - 50%
        TurntablePrize prize1 = new TurntablePrize();
        prize1.setId(1L);
        prize1.setQuality(1);
        prize1.setProbability(500);
        prizes.add(prize1);

        // 稀有品质奖品 - 30%
        TurntablePrize prize2 = new TurntablePrize();
        prize2.setId(2L);
        prize2.setQuality(2);
        prize2.setProbability(300);
        prizes.add(prize2);

        // 史诗品质奖品 - 15%
        TurntablePrize prize3 = new TurntablePrize();
        prize3.setId(3L);
        prize3.setQuality(3);
        prize3.setProbability(150);
        prizes.add(prize3);

        // 传说品质奖品 - 5%
        TurntablePrize prize4 = new TurntablePrize();
        prize4.setId(4L);
        prize4.setQuality(4);
        prize4.setProbability(50);
        prizes.add(prize4);

        return prizes;
    }
}
