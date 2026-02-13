package com.cong.fishisland.service.impl.fund;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.fund.FundMapper;
import com.cong.fishisland.model.dto.fund.AddFundRequest;
import com.cong.fishisland.model.dto.fund.DeleteFundRequest;
import com.cong.fishisland.model.dto.fund.EditFundRequest;
import com.cong.fishisland.model.dto.fund.UpdateFundRequest;
import com.cong.fishisland.model.entity.fund.Fund;
import com.cong.fishisland.model.vo.fund.FundItemVO;
import com.cong.fishisland.model.vo.fund.FundListVO;
import com.cong.fishisland.model.vo.fund.MarketIndexVO;
import com.cong.fishisland.service.fund.FundDataService;
import com.cong.fishisland.service.FundService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shing
 * @description 针对表【fund(基金持仓表)】的数据库操作Service实现
 */
@Service
@Slf4j
public class FundServiceImpl extends ServiceImpl<FundMapper, Fund> implements FundService {

    // ==================== 常量定义 ====================

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCALE_2 = 2;
    private static final int SCALE_4 = 4;
    private static final String DEFAULT_FUND_NAME_PREFIX = "基金";
    private static final String DEFAULT_UNKNOWN_NAME = "未知";
    private static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 国内核心指数配置
     */
    private static final String[][] MAJOR_INDICES = {
            {"sh000001", "上证指数"},
            {"sz399001", "深证成指"},
            {"sz399006", "创业板指"},
            {"sh000300", "沪深300"},
            {"sh000016", "上证50"}
    };

    @Resource
    private FundDataService fundDataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addFund(AddFundRequest addFundRequest, Long userId) {
        validateAddFundRequest(addFundRequest);

        String code = addFundRequest.getCode();
        BigDecimal amount = addFundRequest.getAmount();
        BigDecimal profit = addFundRequest.getProfit();

        // 获取基金实时数据用于计算份额和成本
        JSONObject fundData = fetchFundDataSafely(code);
        BigDecimal currentPrice = extractCurrentPrice(fundData);
        String name = extractFundName(fundData, code);

        // 计算持仓数据
        BigDecimal shares = amount.divide(currentPrice, SCALE_2, RoundingMode.HALF_UP);
        BigDecimal cost = calculateCost(amount, profit, shares);

        // 获取或创建用户基金记录
        Fund fund = getOrCreateUserFund(userId);
        JSONArray fundList = parseFundList(fund);

        // 添加或更新基金
        addOrUpdateFundInList(fundList, code, name, shares, cost);

        fund.setFundJson(JSON.toJSONString(fundList));

        return this.saveOrUpdate(fund);
    }

    /**
     * 校验添加基金请求参数
     */
    private void validateAddFundRequest(AddFundRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String code = request.getCode();
        BigDecimal amount = request.getAmount();
        BigDecimal profit = request.getProfit();

        if (StringUtils.isBlank(code) || amount == null || profit == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "基金代码、持有金额和盈亏金额均为必填");
        }
        if (amount.compareTo(ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "持有金额必须大于 0");
        }
    }

    /**
     * 安全地获取基金数据（失败不抛异常）
     */
    private JSONObject fetchFundDataSafely(String code) {
        try {
            JSONObject data = fundDataService.fetchFromSina(code);
            return data != null ? data : new JSONObject();
        } catch (Exception e) {
            log.warn("获取基金实时数据失败, code: {}, error: {}", code, e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * 从基金数据中提取当前价格
     */
    private BigDecimal extractCurrentPrice(JSONObject fundData) {
        if (fundData != null && !fundData.isEmpty()) {
            BigDecimal price = BigDecimal.valueOf(fundData.getDoubleValue("gsz"));
            if (price.compareTo(ZERO) > 0) {
                return price;
            }
        }
        return ONE;
    }

    /**
     * 从基金数据中提取基金名称
     */
    private String extractFundName(JSONObject fundData, String code) {
        if (fundData != null && !fundData.isEmpty()) {
            String name = fundData.getString("name");
            if (name != null) {
                return name;
            }
        }
        return DEFAULT_FUND_NAME_PREFIX + code;
    }

    /**
     * 计算成本净值
     */
    private BigDecimal calculateCost(BigDecimal amount, BigDecimal profit, BigDecimal shares) {
        BigDecimal principal = amount.subtract(profit);
        if (shares.compareTo(ZERO) <= 0 || principal.compareTo(ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "金额与盈亏组合不合理，无法反推成本净值");
        }
        return principal.divide(shares, SCALE_4, RoundingMode.HALF_UP);
    }

    /**
     * 获取或创建用户基金记录
     */
    private Fund getOrCreateUserFund(Long userId) {
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>().eq(Fund::getUserId, userId));
        if (fund == null) {
            fund = new Fund();
            fund.setUserId(userId);
        }
        return fund;
    }

    /**
     * 解析基金列表JSON
     */
    private JSONArray parseFundList(Fund fund) {
        if (fund.getFundJson() == null) {
            return new JSONArray();
        }
        String fundJson = fund.getFundJson();
        return StringUtils.isNotBlank(fundJson) ? JSON.parseArray(fundJson) : new JSONArray();
    }

    /**
     * 在列表中添加或更新基金
     */
    private void addOrUpdateFundInList(JSONArray fundList, String code, String name, BigDecimal shares, BigDecimal cost) {
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                // 已存在，更新
                item.put("name", name);
                item.put("shares", shares);
                item.put("cost", cost);
                return;
            }
        }

        // 不存在，添加新基金
        JSONObject newFund = new JSONObject();
        newFund.put("code", code);
        newFund.put("name", name);
        newFund.put("shares", shares);
        newFund.put("cost", cost);
        fundList.add(newFund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFund(DeleteFundRequest deleteFundRequest, Long userId) {
        // 参数校验
        if (deleteFundRequest == null || StringUtils.isBlank(deleteFundRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "基金代码不能为空");
        }

        String code = deleteFundRequest.getCode();

        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>()
                .eq(Fund::getUserId, userId));

        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到基金持仓记录");
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        JSONArray newFundList = new JSONArray();
        boolean found = false;

        // 过滤掉要删除的基金，同时检查是否存在
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                found = true; // 找到要删除的基金，不添加到新列表
            } else {
                newFundList.add(item);
            }
        }

        if (!found) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到指定的基金记录");
        }

        fund.setFundJson(JSON.toJSONString(newFundList));
        // updateById 是 MyBatis-Plus 的方法，在当前事务中执行
        return this.updateById(fund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean editFund(EditFundRequest editFundRequest, Long userId) {
        validateEditFundRequest(editFundRequest);

        String code = editFundRequest.getCode();
        BigDecimal amount = editFundRequest.getAmount();
        BigDecimal profit = editFundRequest.getProfit();

        // 获取用户的基金记录
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>().eq(Fund::getUserId, userId));
        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到基金持仓记录");
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        boolean found = updateFundInList(fundList, code, amount, profit);

        if (!found) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到指定的基金记录");
        }

        fund.setFundJson(JSON.toJSONString(fundList));
        return this.updateById(fund);
    }

    /**
     * 校验编辑基金请求参数
     */
    private void validateEditFundRequest(EditFundRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String code = request.getCode();
        BigDecimal amount = request.getAmount();
        BigDecimal profit = request.getProfit();

        if (StringUtils.isBlank(code) || amount == null || profit == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "基金代码、持有金额和盈亏金额均为必填");
        }
        if (amount.compareTo(ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "持有金额必须大于 0");
        }
    }

    /**
     * 在列表中更新基金信息
     */
    private boolean updateFundInList(JSONArray fundList, String code, BigDecimal amount, BigDecimal profit) {
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                updateSingleFundItem(item, code, amount, profit);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新单个基金项的信息
     */
    private void updateSingleFundItem(JSONObject item, String code, BigDecimal amount, BigDecimal profit) {
        // 获取基金实时数据重新计算份额和成本
        JSONObject fundData = fetchFundDataSafely(code);
        BigDecimal currentPrice = extractCurrentPrice(fundData);
        String name = item.getString("name");

        if (!fundData.isEmpty() && fundData.getString("name") != null) {
            name = fundData.getString("name");
        }

        // 重新计算持仓数据
        BigDecimal shares = amount.divide(currentPrice, SCALE_2, RoundingMode.HALF_UP);
        BigDecimal cost = calculateCost(amount, profit, shares);

        // 更新基金信息
        item.put("name", name);
        item.put("shares", shares);
        item.put("cost", cost);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFund(UpdateFundRequest updateFundRequest) {
        validateUpdateFundRequest(updateFundRequest);

        Long userId = updateFundRequest.getUserId();
        String code = updateFundRequest.getCode();

        // 获取用户的基金记录
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>().eq(Fund::getUserId, userId));
        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到基金持仓记录");
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        boolean found = updateAdminFundInList(fundList, code, updateFundRequest);

        if (!found) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到指定的基金记录");
        }

        fund.setFundJson(JSON.toJSONString(fundList));
        return this.updateById(fund);
    }

    /**
     * 校验更新基金请求参数
     */
    private void validateUpdateFundRequest(UpdateFundRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (request.getUserId() == null || StringUtils.isBlank(request.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID和基金代码为必填");
        }
    }

    /**
     * 管理员更新基金信息
     */
    private boolean updateAdminFundInList(JSONArray fundList, String code, UpdateFundRequest request) {
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                updateAdminFundFields(item, request);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新管理员指定的字段
     */
    private void updateAdminFundFields(JSONObject item, UpdateFundRequest request) {
        if (StringUtils.isNotBlank(request.getName())) {
            item.put("name", request.getName());
        }
        if (request.getShares() != null && request.getShares().compareTo(ZERO) > 0) {
            item.put("shares", request.getShares());
        }
        if (request.getCost() != null && request.getCost().compareTo(ZERO) > 0) {
            item.put("cost", request.getCost());
        }
    }

    @Override
    public FundListVO getFundList(Long userId) {
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>().eq(Fund::getUserId, userId));

        // 如果没有持仓记录，返回空结果
        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            return buildEmptyFundListVO();
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        List<FundItemVO> fundItemList = new ArrayList<>();

        // 累计统计数据
        FundStatistics statistics = new FundStatistics();

        // 处理每个基金
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject fundItem = fundList.getJSONObject(i);
            FundItemVO fundItemVO = processSingleFund(fundItem, statistics);
            fundItemList.add(fundItemVO);
        }

        // 构建返回结果
        return buildFundListVO(fundItemList, statistics);
    }

    /**
     * 构建空的基金列表VO
     */
    private FundListVO buildEmptyFundListVO() {
        FundListVO result = new FundListVO();
        result.setFundList(new ArrayList<>());
        result.setTotalMarketValue(ZERO);
        result.setTotalDayProfit(ZERO);
        result.setTotalDayProfitRate(ZERO);
        result.setTotalProfit(ZERO);
        result.setTotalProfitRate(ZERO);
        result.setTodayUpCount(0);
        result.setTodayDownCount(0);
        return result;
    }

    /**
     * 处理单个基金，计算各项指标
     */
    private FundItemVO processSingleFund(JSONObject fundItem, FundStatistics statistics) {
        String code = fundItem.getString("code");
        String name = fundItem.getString("name");
        BigDecimal shares = fundItem.getBigDecimal("shares");
        BigDecimal cost = fundItem.getBigDecimal("cost");

        // 获取实时行情数据
        FundRealTimeData realTimeData = fetchRealTimeData(code, name, cost);

        // 计算各项指标
        FundCalculations calculations = calculateFundMetrics(shares, cost, realTimeData);

        // 更新统计数据
        statistics.accumulate(calculations);

        // 构建VO
        return buildFundItemVO(code, realTimeData.name, shares, cost, realTimeData, calculations);
    }

    /**
     * 获取基金实时数据
     */
    private FundRealTimeData fetchRealTimeData(String code, String name, BigDecimal cost) {
        BigDecimal currentPrice = cost != null ? cost : ZERO;
        BigDecimal prevPrice = cost != null ? cost : ZERO;
        BigDecimal changePercent = ZERO;
        String fundName = name;

        try {
            JSONObject fundData = fundDataService.getBestFundData(code);
            if (fundData != null && !fundData.isEmpty()) {
                currentPrice = BigDecimal.valueOf(fundData.getDoubleValue("gsz"));
                prevPrice = BigDecimal.valueOf(fundData.getDoubleValue("dwjz"));
                changePercent = BigDecimal.valueOf(fundData.getDoubleValue("gszzl"));

                String dataName = fundData.getString("name");
                if (StringUtils.isNotBlank(dataName)) {
                    fundName = dataName;
                }
            }
        } catch (Exception e) {
            log.warn("获取基金数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
        }

        if (StringUtils.isBlank(fundName)) {
            fundName = DEFAULT_UNKNOWN_NAME;
        }

        return new FundRealTimeData(fundName, currentPrice, prevPrice, changePercent);
    }

    /**
     * 计算基金各项指标
     */
    private FundCalculations calculateFundMetrics(BigDecimal shares, BigDecimal cost, FundRealTimeData realTimeData) {
        // 1. 持有市值 = shares * currentPrice
        BigDecimal marketValue = shares.multiply(realTimeData.currentPrice);

        // 2. 今日盈亏 = (currentPrice - prevPrice) * shares
        BigDecimal dayProfit = realTimeData.currentPrice.subtract(realTimeData.prevPrice).multiply(shares);

        // 3. 累计盈亏 = (currentPrice - cost) * shares
        BigDecimal totalProfit = realTimeData.currentPrice.subtract(cost).multiply(shares);

        // 4. 持有收益率 = (currentPrice - cost) / cost * 100
        BigDecimal profitRate = ZERO;
        if (cost.compareTo(ZERO) > 0) {
            profitRate = realTimeData.currentPrice.subtract(cost)
                    .divide(cost, SCALE_4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED);
        }

        return new FundCalculations(marketValue, dayProfit, totalProfit, profitRate);
    }

    /**
     * 构建基金项VO
     */
    private FundItemVO buildFundItemVO(String code, String name, BigDecimal shares, BigDecimal cost,
                                       FundRealTimeData realTimeData, FundCalculations calculations) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT));

        FundItemVO vo = new FundItemVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setShares(shares);
        vo.setCost(cost);
        vo.setCurrentPrice(realTimeData.currentPrice);
        vo.setPrevPrice(realTimeData.prevPrice);
        vo.setChangePercent(realTimeData.changePercent.setScale(SCALE_2, RoundingMode.HALF_UP));
        vo.setMarketValue(calculations.marketValue.setScale(SCALE_2, RoundingMode.HALF_UP));
        vo.setDayProfit(calculations.dayProfit.setScale(SCALE_2, RoundingMode.HALF_UP));
        vo.setTotalProfit(calculations.totalProfit.setScale(SCALE_2, RoundingMode.HALF_UP));
        vo.setProfitRate(calculations.profitRate.setScale(SCALE_2, RoundingMode.HALF_UP));
        vo.setUpdateTime(currentTime);

        return vo;
    }

    /**
     * 构建基金列表VO
     */
    private FundListVO buildFundListVO(List<FundItemVO> fundItemList, FundStatistics statistics) {
        // 计算今日总收益率 = 今日总盈亏 / 昨日总市值 * 100
        BigDecimal totalDayProfitRate = ZERO;
        BigDecimal yesterdayTotalMarketValue = statistics.totalMarketValue.subtract(statistics.totalDayProfit);
        if (yesterdayTotalMarketValue.compareTo(ZERO) > 0) {
            totalDayProfitRate = statistics.totalDayProfit
                    .divide(yesterdayTotalMarketValue, SCALE_4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED);
        }

        // 计算总持有收益率 = 累计总盈亏 / 总成本 * 100
        BigDecimal totalProfitRate = ZERO;
        BigDecimal totalCost = statistics.totalMarketValue.subtract(statistics.totalProfit);
        if (totalCost.compareTo(ZERO) > 0) {
            totalProfitRate = statistics.totalProfit
                    .divide(totalCost, SCALE_4, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED);
        }

        FundListVO result = new FundListVO();
        result.setFundList(fundItemList);
        result.setTotalMarketValue(statistics.totalMarketValue.setScale(SCALE_2, RoundingMode.HALF_UP));
        result.setTotalDayProfit(statistics.totalDayProfit.setScale(SCALE_2, RoundingMode.HALF_UP));
        result.setTotalDayProfitRate(totalDayProfitRate.setScale(SCALE_2, RoundingMode.HALF_UP));
        result.setTotalProfit(statistics.totalProfit.setScale(SCALE_2, RoundingMode.HALF_UP));
        result.setTotalProfitRate(totalProfitRate.setScale(SCALE_2, RoundingMode.HALF_UP));
        result.setTodayUpCount(statistics.todayUpCount);
        result.setTodayDownCount(statistics.todayDownCount);

        return result;
    }

    // ==================== 内部类 ====================

    /**
     * 基金实时数据内部类
     */
    private static class FundRealTimeData {
        String name;
        BigDecimal currentPrice;
        BigDecimal prevPrice;
        BigDecimal changePercent;

        FundRealTimeData(String name, BigDecimal currentPrice, BigDecimal prevPrice, BigDecimal changePercent) {
            this.name = name;
            this.currentPrice = currentPrice;
            this.prevPrice = prevPrice;
            this.changePercent = changePercent;
        }
    }

    /**
     * 基金计算结果内部类
     */
    private static class FundCalculations {
        BigDecimal marketValue;
        BigDecimal dayProfit;
        BigDecimal totalProfit;
        BigDecimal profitRate;

        FundCalculations(BigDecimal marketValue, BigDecimal dayProfit, BigDecimal totalProfit, BigDecimal profitRate) {
            this.marketValue = marketValue;
            this.dayProfit = dayProfit;
            this.totalProfit = totalProfit;
            this.profitRate = profitRate;
        }
    }

    /**
     * 基金统计数据内部类
     */
    private static class FundStatistics {
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalDayProfit = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int todayUpCount = 0;
        int todayDownCount = 0;

        void accumulate(FundCalculations calculations) {
            totalMarketValue = totalMarketValue.add(calculations.marketValue);
            totalDayProfit = totalDayProfit.add(calculations.dayProfit);
            totalProfit = totalProfit.add(calculations.totalProfit);

            // 统计今日涨跌数量
            if (calculations.dayProfit.compareTo(BigDecimal.ZERO) > 0) {
                todayUpCount++;
            } else if (calculations.dayProfit.compareTo(BigDecimal.ZERO) < 0) {
                todayDownCount++;
            }
        }
    }

    @Override
    public List<MarketIndexVO> getMajorIndices() {
        List<MarketIndexVO> result = new ArrayList<>();

        for (String[] index : MAJOR_INDICES) {
            String code = index[0];
            String defaultName = index[1];

            try {
                MarketIndexVO vo = fetchAndBuildIndexVO(code, defaultName);
                result.add(vo);
            } catch (Exception e) {
                log.error("处理指数数据异常 - 指数: {}, 错误: {}", defaultName, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 获取并构建指数VO
     */
    private MarketIndexVO fetchAndBuildIndexVO(String code, String defaultName) {
        JSONObject indexData = fundDataService.fetchIndexData(code);

        if (indexData != null && !indexData.isEmpty()) {
            return buildIndexVOFromData(indexData, code, defaultName);
        } else {
            log.warn("获取指数数据失败，使用默认值 - 指数: {}", defaultName);
            return buildDefaultIndexVO(code, defaultName);
        }
    }

    /**
     * 从数据构建指数VO
     */
    private MarketIndexVO buildIndexVOFromData(JSONObject indexData, String code, String defaultName) {
        MarketIndexVO vo = new MarketIndexVO();
        vo.setIndexCode(code);
        vo.setIndexName(indexData.getString("name") != null ? indexData.getString("name") : defaultName);
        vo.setCurrentValue(BigDecimal.valueOf(indexData.getDoubleValue("current")).setScale(SCALE_2, RoundingMode.HALF_UP));
        vo.setChangeValue(BigDecimal.valueOf(indexData.getDoubleValue("change")).setScale(SCALE_2, RoundingMode.HALF_UP));

        // 格式化涨跌幅，带上%符号
        BigDecimal changePct = BigDecimal.valueOf(indexData.getDoubleValue("changePct")).setScale(SCALE_2, RoundingMode.HALF_UP);
        vo.setChangePercent(changePct.toPlainString() + "%");

        return vo;
    }

    /**
     * 构建默认指数VO
     */
    private MarketIndexVO buildDefaultIndexVO(String code, String defaultName) {
        MarketIndexVO vo = new MarketIndexVO();
        vo.setIndexCode(code);
        vo.setIndexName(defaultName);
        vo.setCurrentValue(ZERO);
        vo.setChangeValue(ZERO);
        vo.setChangePercent("0.00%");
        return vo;
    }
}




