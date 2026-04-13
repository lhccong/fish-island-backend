package com.cong.fishisland.service.impl.fund;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.fund.IndexTradeMapper;
import com.cong.fishisland.model.entity.fund.IndexPosition;
import com.cong.fishisland.model.entity.fund.IndexTradeRecord;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.fund.IndexPositionVO;
import com.cong.fishisland.model.vo.fund.IndexTradeResultVO;
import com.cong.fishisland.model.vo.fund.IndexTransactionVO;
import com.cong.fishisland.service.IndexPositionService;
import com.cong.fishisland.service.IndexTradeService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.fund.FundDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指数交易服务实现
 *
 * @author shing
 */
@Service
@Slf4j
public class IndexTradeServiceImpl extends ServiceImpl<IndexTradeMapper, IndexTradeRecord>
        implements IndexTradeService {

    @Resource
    private IndexPositionService positionService;

    @Resource
    private UserPointsService userPointsService;

    @Resource
    private FundDataService fundDataService;

    // ==================== 常量定义 ====================

    private static final int TRADE_TYPE_BUY = 1;
    private static final int TRADE_TYPE_SELL = 2;
    private static final int STATUS_COMPLETED = 1;

    private static final int MIN_BUY_AMOUNT = 100;
    private static final BigDecimal MIN_SHARES = new BigDecimal("0.0001");

    private static final LocalTime TRADE_START = LocalTime.of(9, 30);
    private static final LocalTime TRADE_END = LocalTime.of(15, 0);
    
    // 积分记录来源类型
    private static final String INDEX_BUY = "index_buy";
    private static final String INDEX_SELL = "index_sell";

    // ==================== 交易操作（对外接口） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IndexTradeResultVO buyIndexWithResult(Long userId, String indexCode, Long amount) {
        checkTradeTime();
        BigDecimal currentNav = getCurrentNav(indexCode);
        BigDecimal shares = calculateShares(amount, currentNav);
        Long tradeId = executeBuy(userId, indexCode, amount, shares, currentNav);
        return buildBuyResult(tradeId, amount, shares, currentNav);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IndexTradeResultVO sellIndexWithResult(Long userId, String indexCode, BigDecimal shares) {
        checkTradeTime();
        BigDecimal currentNav = getCurrentNav(indexCode);
        Long amount = calculateAmount(shares, currentNav);
        Long tradeId = executeSell(userId, indexCode, shares, amount, currentNav);
        return buildSellResult(tradeId, shares, amount, currentNav);
    }

    @Override
    public IndexPositionVO getUserPosition(Long userId, String indexCode) {
        IndexPosition position = positionService.getOrCreatePosition(userId, indexCode);
        BigDecimal currentNav = getCurrentNav(indexCode);
        return buildPositionVO(position, currentNav);
    }

    @Override
    public Page<IndexTransactionVO> getUserTransactionPage(Long userId, String indexCode, Long current, Long pageSize) {
        List<IndexTradeRecord> records = queryUserTransactions(userId, indexCode);
        List<IndexTransactionVO> voList = convertToVOList(records);

        Page<IndexTransactionVO> page = new Page<>(current, pageSize);
        page.setRecords(voList);
        page.setTotal(voList.size());
        return page;
    }

    @Override
    public List<IndexTransactionVO> getUserPendingTransactions(Long userId, String indexCode) {
        return new java.util.ArrayList<>();
    }

    @Override
    public List<IndexTradeRecord> getUserTransactions(Long userId, String indexCode) {
        return queryUserTransactions(userId, indexCode);
    }

    // ==================== 买入流程（私有方法） ====================

    /**
     * 执行买入操作
     */
    private Long executeBuy(Long userId, String indexCode, Long amount, BigDecimal shares, BigDecimal nav) {
        deductUserPoints(userId, amount);
        positionService.addLockedShares(userId, indexCode, shares, nav);
        return saveBuyRecord(userId, indexCode, amount, shares, nav);
    }

    /**
     * 计算买入份额
     */
    private BigDecimal calculateShares(Long amount, BigDecimal nav) {
        if (amount < MIN_BUY_AMOUNT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "买入金额不能低于" + MIN_BUY_AMOUNT + "积分");
        }

        BigDecimal shares = new BigDecimal(amount).divide(nav, 4, RoundingMode.DOWN);

        if (shares.compareTo(MIN_SHARES) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "买入份额过小，请增加投入金额");
        }

        return shares;
    }

    /**
     * 扣除用户积分
     */
    private void deductUserPoints(Long userId, Long amount) {
        userPointsService.deductPoints(
                userId,
                amount.intValue(),
                INDEX_BUY,
                null,
                "指数买入"
        );
    }

    /**
     * 保存买入记录
     */
    private Long saveBuyRecord(Long userId, String indexCode, Long amount, BigDecimal shares, BigDecimal nav) {
        IndexTradeRecord record = new IndexTradeRecord();
        record.setUserId(userId);
        record.setIndexCode(indexCode);
        record.setTradeType(TRADE_TYPE_BUY);
        record.setAmount(amount);
        record.setNav(nav);
        record.setShares(shares);
        record.setStatus(STATUS_COMPLETED);
        record.setCreateTime(new Date());

        this.save(record);
        log.info("买入记录已保存 - 用户: {}, 指数: {}, 金额: {}, 份额: {}", userId, indexCode, amount, shares);

        return record.getId();
    }

    // ==================== 卖出流程（私有方法） ====================

    /**
     * 执行卖出操作
     */
    private Long executeSell(Long userId, String indexCode, BigDecimal shares, Long amount, BigDecimal nav) {
        if (shares.compareTo(MIN_SHARES) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "卖出份额不能低于" + MIN_SHARES);
        }

        boolean success = positionService.reduceAvailableShares(userId, indexCode, shares);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "可用份额不足");
        }

        returnUserPoints(userId, amount);
        Long profitLoss = calculateProfitLoss(userId, indexCode, shares, nav);
        return saveSellRecord(userId, indexCode, shares, amount, nav, profitLoss);
    }

    /**
     * 计算卖出金额
     */
    private Long calculateAmount(BigDecimal shares, BigDecimal nav) {
        return shares.multiply(nav).setScale(0, RoundingMode.DOWN).longValue();
    }

    /**
     * 返还用户积分（T+0）
     */
    private void returnUserPoints(Long userId, Long amount) {
        userPointsService.updateUsedPoints(
                userId,
                -amount.intValue(),
                INDEX_SELL,
                null,
                "指数卖出"
        );
    }

    /**
     * 计算盈亏
     */
    private Long calculateProfitLoss(Long userId, String indexCode, BigDecimal shares, BigDecimal currentNav) {
        IndexPosition position = positionService.getOrCreatePosition(userId, indexCode);
        
        // 防止成本为0的情况
        if (position.getAvgCost().compareTo(BigDecimal.ZERO) == 0) {
            return 0L;
        }
        
        BigDecimal costValue = shares.multiply(position.getAvgCost());
        BigDecimal currentValue = shares.multiply(currentNav);
        return currentValue.subtract(costValue).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 保存卖出记录
     */
    private Long saveSellRecord(Long userId, String indexCode, BigDecimal shares, Long amount,
                                BigDecimal nav, Long profitLoss) {
        IndexTradeRecord record = new IndexTradeRecord();
        record.setUserId(userId);
        record.setIndexCode(indexCode);
        record.setTradeType(TRADE_TYPE_SELL);
        record.setAmount(amount);
        record.setNav(nav);
        record.setShares(shares);
        record.setStatus(STATUS_COMPLETED);
        record.setProfitLoss(profitLoss);
        record.setActualSettleTime(new Date());
        record.setCreateTime(new Date());

        this.save(record);
        log.info("卖出记录已保存 - 用户: {}, 指数: {}, 份额: {}, 金额: {}, 盈亏: {}",
                userId, indexCode, shares, amount, profitLoss);

        return record.getId();
    }

    // ==================== 查询辅助方法 ====================

    /**
     * 校验交易时间
     */
    private void checkTradeTime() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Shanghai"));
        if (now.isBefore(TRADE_START) || now.isAfter(TRADE_END)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "非交易时间，交易时间为 09:30-15:00");
        }
    }

    /**
     * 获取当前净值
     */
    private BigDecimal getCurrentNav(String indexCode) {
        try {
            JSONObject indexData = fundDataService.fetchIndexData(indexCode);
            if (indexData == null || !indexData.containsKey("current")) {
                log.error("获取指数数据失败 - 指数: {}, 返回数据为空或缺少 current 字段", indexCode);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取指数数据失败");
            }

            Double currentPrice = indexData.getDouble("current");
            if (currentPrice == null || currentPrice <= 0) {
                log.error("获取指数净值失败 - 指数: {}, 价格无效: {}", indexCode, currentPrice);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "指数价格无效");
            }
            
            return new BigDecimal(currentPrice).divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取指数净值异常 - 指数: {}", indexCode, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取指数净值失败");
        }
    }

    /**
     * 查询用户交易记录
     */
    private List<IndexTradeRecord> queryUserTransactions(Long userId, String indexCode) {
        LambdaQueryWrapper<IndexTradeRecord> query = new LambdaQueryWrapper<>();
        query.eq(IndexTradeRecord::getUserId, userId)
                .eq(IndexTradeRecord::getIndexCode, indexCode)
                .orderByDesc(IndexTradeRecord::getCreateTime);
        return this.list(query);
    }

    // ==================== VO 构建方法 ====================

    /**
     * 构建买入结果 VO
     */
    private IndexTradeResultVO buildBuyResult(Long tradeId, Long amount, BigDecimal shares, BigDecimal nav) {
        IndexTradeResultVO result = new IndexTradeResultVO();
        result.setTransactionId(tradeId);
        result.setTradeType(TRADE_TYPE_BUY);
        result.setTradeTypeName("买入");
        result.setAmount(amount);
        result.setShares(shares);
        result.setNav(nav);
        result.setMessage("买入成功，份额已到账");
        return result;
    }

    /**
     * 构建卖出结果 VO
     */
    private IndexTradeResultVO buildSellResult(Long tradeId, BigDecimal shares, Long amount, BigDecimal nav) {
        IndexTradeResultVO result = new IndexTradeResultVO();
        result.setTransactionId(tradeId);
        result.setTradeType(TRADE_TYPE_SELL);
        result.setTradeTypeName("卖出");
        result.setShares(shares);
        result.setAmount(amount);
        result.setNav(nav);
        result.setMessage("卖出成功，积分已到账");
        return result;
    }

    /**
     * 构建持仓 VO
     */
    private IndexPositionVO buildPositionVO(IndexPosition position, BigDecimal currentNav) {
        IndexPositionVO vo = new IndexPositionVO();
        vo.setIndexCode(position.getIndexCode());
        vo.setTotalShares(position.getTotalShares());
        vo.setAvailableShares(position.getAvailableShares());
        vo.setLockedShares(position.getLockedShares());
        vo.setAvgCost(position.getAvgCost());
        vo.setCurrentNav(currentNav);

        if (position.getTotalShares().compareTo(BigDecimal.ZERO) > 0) {
            calculateAndSetProfitInfo(vo, position, currentNav);
        } else {
            setZeroProfitInfo(vo);
        }

        return vo;
    }

    /**
     * 计算并设置盈亏信息
     */
    private void calculateAndSetProfitInfo(IndexPositionVO vo, IndexPosition position, BigDecimal currentNav) {
        BigDecimal totalShares = position.getTotalShares();
        BigDecimal avgCost = position.getAvgCost();
        
        // 计算市值和成本
        BigDecimal marketValue = totalShares.multiply(currentNav);
        BigDecimal costValue = totalShares.multiply(avgCost);
        BigDecimal totalProfit = marketValue.subtract(costValue);
        
        // 防止除零：成本为0时，收益率和涨跌幅设为0
        BigDecimal profitRate = BigDecimal.ZERO;
        BigDecimal changePercent = BigDecimal.ZERO;
        
        if (costValue.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = totalProfit.divide(costValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        
        if (avgCost.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = currentNav.subtract(avgCost)
                    .divide(avgCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        vo.setMarketValue(marketValue);
        vo.setTotalProfit(totalProfit);
        vo.setProfitRate(profitRate);
        vo.setChangePercent(changePercent);
    }

    /**
     * 设置零盈亏信息
     */
    private void setZeroProfitInfo(IndexPositionVO vo) {
        vo.setMarketValue(BigDecimal.ZERO);
        vo.setTotalProfit(BigDecimal.ZERO);
        vo.setProfitRate(BigDecimal.ZERO);
        vo.setChangePercent(BigDecimal.ZERO);
    }

    /**
     * 转换为 VO 列表
     */
    private List<IndexTransactionVO> convertToVOList(List<IndexTradeRecord> records) {
        return records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 VO
     */
    private IndexTransactionVO convertToVO(IndexTradeRecord record) {
        IndexTransactionVO vo = new IndexTransactionVO();
        vo.setId(record.getId());
        vo.setIndexCode(record.getIndexCode());
        vo.setTradeType(record.getTradeType());
        vo.setTradeTypeName(record.getTradeType() == TRADE_TYPE_BUY ? "买入" : "卖出");
        vo.setAmount(record.getAmount());
        vo.setNav(record.getNav());
        vo.setShares(record.getShares());
        vo.setStatus(record.getStatus());
        vo.setStatusName(record.getStatus() == STATUS_COMPLETED ? "已完成" : "待确认");
        vo.setProfitLoss(record.getProfitLoss());

        if (record.getCreateTime() != null) {
            vo.setCreateTime(record.getCreateTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        if (record.getActualSettleTime() != null) {
            vo.setActualSettleTime(record.getActualSettleTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        return vo;
    }
}
