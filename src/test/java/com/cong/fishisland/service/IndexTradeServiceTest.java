package com.cong.fishisland.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.TestBaseByLogin;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.entity.fund.IndexTradeRecord;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.vo.fund.IndexPositionVO;
import com.cong.fishisland.model.vo.fund.IndexTradeResultVO;
import com.cong.fishisland.model.vo.fund.IndexTransactionVO;
import com.cong.fishisland.service.fund.FundDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 指数交易服务测试
 * 注意：交易时间限制测试需要在 09:30-15:00 之间运行
 *
 * @author shing
 */
@Transactional
class IndexTradeServiceTest extends TestBaseByLogin {

    @Resource
    private IndexTradeService indexTradeService;

    @Resource
    private UserPointsService userPointsService;

    @MockBean
    private FundDataService fundDataService;

    // 与 TestBaseByLogin 中 mockUser.getId() 保持一致
    private static final Long TEST_USER_ID = 1816001696590692353L;
    private static final String TEST_INDEX_CODE = "sh000001";
    private static final BigDecimal TEST_NAV = new BigDecimal("3.5000");
    private static final Long TEST_BUY_AMOUNT = 1000L;

    @BeforeEach
    void setUp() {
        // Mock 指数数据服务返回固定净值
        JSONObject mockIndexData = new JSONObject();
        mockIndexData.put("current", 3500.0); // 指数点位 3500，净值为 3.5
        when(fundDataService.fetchIndexData(anyString())).thenReturn(mockIndexData);

        // 初始化测试用户积分记录（若不存在则插入）
        if (userPointsService.getById(TEST_USER_ID) == null) {
            UserPoints userPoints = new UserPoints();
            userPoints.setUserId(TEST_USER_ID);
            userPoints.setPoints(100000);
            userPoints.setUsedPoints(0);
            userPoints.setLevel(1);
            userPointsService.save(userPoints);
        }
    }

    /**
     * 测试买入指数 - 正常流程
     * 注意：需要在交易时间（09:30-15:00）运行
     */
    @Test
    void testBuyIndex_Success() {
        try {
            IndexTradeResultVO result = indexTradeService.buyIndexWithResult(
                    TEST_USER_ID, TEST_INDEX_CODE, TEST_BUY_AMOUNT);

            assertNotNull(result);
            assertEquals(1, result.getTradeType());
            assertEquals("买入", result.getTradeTypeName());
            assertEquals(TEST_BUY_AMOUNT, result.getAmount());
            assertEquals(TEST_NAV, result.getNav());
            assertNotNull(result.getShares());
            assertTrue(result.getShares().compareTo(BigDecimal.ZERO) > 0);
            assertEquals("买入成功，份额已到账", result.getMessage());
        } catch (BusinessException e) {
            if (e.getMessage().contains("非交易时间")) {
                System.out.println("跳过测试：当前不在交易时间内（09:30-15:00）");
            } else {
                throw e;
            }
        }
    }

    /**
     * 测试买入指数 - 金额过低
     */
    @Test
    void testBuyIndex_AmountTooLow() {
        try {
            assertThrows(BusinessException.class, () -> {
                indexTradeService.buyIndexWithResult(TEST_USER_ID, TEST_INDEX_CODE, 50L);
            });
        } catch (BusinessException e) {
            if (e.getMessage().contains("非交易时间")) {
                System.out.println("跳过测试：当前不在交易时间内（09:30-15:00）");
            } else {
                throw e;
            }
        }
    }

    /**
     * 测试获取用户持仓信息 - 无持仓
     */
    @Test
    void testGetUserPosition_NoPosition() {
        IndexPositionVO position = indexTradeService.getUserPosition(999L, TEST_INDEX_CODE);

        assertNotNull(position);
        assertEquals(BigDecimal.ZERO.setScale(4, BigDecimal.ROUND_HALF_UP), position.getTotalShares());
        assertEquals(BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP), position.getMarketValue());
    }

    /**
     * 测试获取交易记录列表
     */
    @Test
    void testGetUserTransactionPage() {
        try {
            // 执行几笔交易
            indexTradeService.buyIndexWithResult(TEST_USER_ID, TEST_INDEX_CODE, TEST_BUY_AMOUNT);
            indexTradeService.buyIndexWithResult(TEST_USER_ID, TEST_INDEX_CODE, 500L);

            // 查询交易记录
            Page<IndexTransactionVO> page = indexTradeService.getUserTransactionPage(
                    TEST_USER_ID, TEST_INDEX_CODE, 1L, 10L);

            assertNotNull(page);
            assertTrue(page.getTotal() >= 2);
            assertFalse(page.getRecords().isEmpty());

            IndexTransactionVO transaction = page.getRecords().get(0);
            assertNotNull(transaction.getId());
            assertEquals(TEST_INDEX_CODE, transaction.getIndexCode());
            assertNotNull(transaction.getTradeTypeName());
        } catch (BusinessException e) {
            if (e.getMessage().contains("非交易时间")) {
                System.out.println("跳过测试：当前不在交易时间内（09:30-15:00）");
            } else {
                throw e;
            }
        }
    }

    /**
     * 测试交易记录查询
     */
    @Test
    void testGetUserTransactions() {
        try {
            // 执行交易
            indexTradeService.buyIndexWithResult(TEST_USER_ID, TEST_INDEX_CODE, TEST_BUY_AMOUNT);

            // 查询记录
            List<IndexTradeRecord> records = indexTradeService.getUserTransactions(TEST_USER_ID, TEST_INDEX_CODE);

            assertNotNull(records);
            assertFalse(records.isEmpty());
            assertEquals(TEST_USER_ID, records.get(0).getUserId());
            assertEquals(TEST_INDEX_CODE, records.get(0).getIndexCode());
        } catch (BusinessException e) {
            if (e.getMessage().contains("非交易时间")) {
                System.out.println("跳过测试：当前不在交易时间内（09:30-15:00）");
            } else {
                throw e;
            }
        }
    }
}
