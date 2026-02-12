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

    @Resource
    private FundDataService fundDataService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addFund(AddFundRequest addFundRequest, Long userId) {
        // 基础参数校验（进一步收紧业务约束）
        if (addFundRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String code = addFundRequest.getCode();
        BigDecimal amount = addFundRequest.getAmount();
        BigDecimal profit = addFundRequest.getProfit();

        if (StringUtils.isBlank(code) || amount == null || profit == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "基金代码、持有金额和盈亏金额均为必填");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "持有金额必须大于 0");
        }

        // 获取基金实时数据用于计算份额和成本
        JSONObject fundData = null;
        try {
            fundData = fundDataService.fetchFromSina(code);
        } catch (Exception e) {
            // 行情失败不影响录入，降级为本地计算
            log.warn("获取基金实时数据失败, code: {}, error: {}", code, e.getMessage());
        }

        BigDecimal currentPrice = BigDecimal.ONE;
        String name = "基金" + code;
        
        if (fundData != null && !fundData.isEmpty()) {
            currentPrice = BigDecimal.valueOf(fundData.getDoubleValue("gsz"));
            if (fundData.getString("name") != null) {
                name = fundData.getString("name");
            }
        }
        
        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            currentPrice = BigDecimal.ONE;
        }

        // 计算持仓事实数据：份额 = 当前金额 / 当前价格
        BigDecimal shares = amount.divide(currentPrice, 2, RoundingMode.HALF_UP);

        // 计算成本净值：投入本金 = 金额 - 盈亏；成本净值 = 本金 / 份额
        BigDecimal principal = amount.subtract(profit);
        if (shares.compareTo(BigDecimal.ZERO) <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "金额与盈亏组合不合理，无法反推成本净值");
        }
        BigDecimal cost = principal.divide(shares, 4, RoundingMode.HALF_UP);

        // 1. 校验用户是否已有 fund 记录
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>()
                .eq(Fund::getUserId, userId));
        
        JSONArray fundList;
        if (fund == null) {
            // 2. 若无，创建 fund 记录（fundJson 初始化为空数组）
            fund = new Fund();
            fund.setUserId(userId);
            fundList = new JSONArray();
        } else {
            // 解析现有的 fundJson
            String fundJson = fund.getFundJson();
            fundList = StringUtils.isNotBlank(fundJson)
                    ? JSON.parseArray(fundJson)
                    : new JSONArray();
        }

        // 3. 检查 fundJson 中是否已存在该基金代码
        boolean found = false;
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                // 若已存在 → 覆盖更新
                item.put("name", name);
                item.put("shares", shares);
                item.put("cost", cost);
                found = true;
                break;
            }
        }
        
        if (!found) {
            // 若不存在 → 追加新基金
            JSONObject newFund = new JSONObject();
            newFund.put("code", code);
            newFund.put("name", name);
            newFund.put("shares", shares);
            newFund.put("cost", cost);
            fundList.add(newFund);
        }

        // 4. 保存 fundJson（注意：这里是业务行为，不是数据库记录行为）
        fund.setFundJson(JSON.toJSONString(fundList));
        return this.saveOrUpdate(fund);
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
        return this.updateById(fund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean editFund(EditFundRequest editFundRequest, Long userId) {
        // 基础参数校验
        if (editFundRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String code = editFundRequest.getCode();
        BigDecimal amount = editFundRequest.getAmount();
        BigDecimal profit = editFundRequest.getProfit();

        if (StringUtils.isBlank(code) || amount == null || profit == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "基金代码、持有金额和盈亏金额均为必填");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "持有金额必须大于 0");
        }

        // 获取用户的基金记录
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>()
                .eq(Fund::getUserId, userId));

        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到基金持仓记录");
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        boolean found = false;

        // 查找要编辑的基金
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                // 获取基金实时数据重新计算份额和成本
                JSONObject fundData = null;
                try {
                    fundData = fundDataService.fetchFromSina(code);
                } catch (Exception e) {
                    log.warn("获取基金实时数据失败, code: {}, error: {}", code, e.getMessage());
                }

                BigDecimal currentPrice = BigDecimal.ONE;
                String name = item.getString("name");

                if (fundData != null && !fundData.isEmpty()) {
                    currentPrice = BigDecimal.valueOf(fundData.getDoubleValue("gsz"));
                    if (fundData.getString("name") != null) {
                        name = fundData.getString("name");
                    }
                }

                if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    currentPrice = BigDecimal.ONE;
                }

                // 重新计算持仓事实数据：份额和成本净值
                BigDecimal shares = amount.divide(currentPrice, 2, RoundingMode.HALF_UP);
                BigDecimal principal = amount.subtract(profit);
                if (shares.compareTo(BigDecimal.ZERO) <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "金额与盈亏组合不合理，无法反推成本净值");
                }
                BigDecimal cost = principal.divide(shares, 4, RoundingMode.HALF_UP);

                // 覆盖 fundJson 中对应基金节点
                item.put("name", name);
                item.put("shares", shares);
                item.put("cost", cost);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到指定的基金记录");
        }

        // 保存更新后的 fundJson
        fund.setFundJson(JSON.toJSONString(fundList));
        return this.updateById(fund);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFund(UpdateFundRequest updateFundRequest) {
        // 基础参数校验
        if (updateFundRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        Long userId = updateFundRequest.getUserId();
        String code = updateFundRequest.getCode();
        String name = updateFundRequest.getName();
        BigDecimal shares = updateFundRequest.getShares();
        BigDecimal cost = updateFundRequest.getCost();

        if (userId == null || StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID和基金代码为必填");
        }

        // 获取用户的基金记录（@TableLogic会自动过滤已删除数据）
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>()
                .eq(Fund::getUserId, userId));

        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到基金持仓记录");
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        boolean found = false;

        // 查找要更新的基金
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject item = fundList.getJSONObject(i);
            if (code.equals(item.getString("code"))) {
                // 管理员可以直接修改所有字段
                if (StringUtils.isNotBlank(name)) {
                    item.put("name", name);
                }
                if (shares != null && shares.compareTo(BigDecimal.ZERO) > 0) {
                    item.put("shares", shares);
                }
                if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                    item.put("cost", cost);
                }
                found = true;
                break;
            }
        }

        if (!found) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到指定的基金记录");
        }

        // 保存到数据库
        fund.setFundJson(JSON.toJSONString(fundList));
        return this.updateById(fund);
    }

    @Override
    public FundListVO getFundList(Long userId) {
        // 获取用户的基金持仓记录（fundJson 仅存储持仓事实）
        Fund fund = this.getOne(new LambdaQueryWrapper<Fund>()
                .eq(Fund::getUserId, userId));

        FundListVO result = new FundListVO();
        List<FundItemVO> fundItemList = new ArrayList<>();

        if (fund == null || StringUtils.isBlank(fund.getFundJson())) {
            result.setFundList(fundItemList);
            result.setTotalMarketValue(BigDecimal.ZERO);
            result.setTotalDayProfit(BigDecimal.ZERO);
            result.setTotalDayProfitRate(BigDecimal.ZERO);
            result.setTotalProfit(BigDecimal.ZERO);
            result.setTotalProfitRate(BigDecimal.ZERO);
            result.setTodayUpCount(0);
            result.setTodayDownCount(0);
            return result;
        }

        JSONArray fundList = JSON.parseArray(fund.getFundJson());
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalDayProfit = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int todayUpCount = 0;
        int todayDownCount = 0;

        // 重要：所有估值数据基于实时行情动态计算，估值过程不修改 fundJson
        for (int i = 0; i < fundList.size(); i++) {
            JSONObject fundItem = fundList.getJSONObject(i);
            String code = fundItem.getString("code");
            String name = fundItem.getString("name");
            BigDecimal shares = fundItem.getBigDecimal("shares");
            BigDecimal cost = fundItem.getBigDecimal("cost");

            // 默认值
            BigDecimal currentPrice = cost != null ? cost : BigDecimal.ZERO;
            BigDecimal prevPrice = cost != null ? cost : BigDecimal.ZERO;
            BigDecimal changePercent = BigDecimal.ZERO;

            // 获取实时数据
            JSONObject fundData = null;
            try {
                fundData = fundDataService.getBestFundData(code);
            } catch (Exception e) {
                log.warn("获取基金数据失败 - 基金代码: {}, 错误: {}", code, e.getMessage());
            }

            if (fundData != null && !fundData.isEmpty()) {
                currentPrice = BigDecimal.valueOf(fundData.getDoubleValue("gsz"));
                prevPrice = BigDecimal.valueOf(fundData.getDoubleValue("dwjz"));
                changePercent = BigDecimal.valueOf(fundData.getDoubleValue("gszzl"));

                // 更新基金名称
                if (fundData.getString("name") != null && StringUtils.isNotBlank(fundData.getString("name"))) {
                    name = fundData.getString("name");
                }
            } else {
                // 如果获取数据失败，使用默认值
                name = fundItem.getString("name");
                if (StringUtils.isBlank(name)) {
                    name = "未知";
                }
            }

            // 计算市值、盈亏（与 app.py 的 process_single_fund 计算公式完全一致）
            // 1. 持有市值 = shares * currentPrice
            BigDecimal marketValue = shares.multiply(currentPrice);

            // 2. 今日盈亏 = (currentPrice - prevPrice) * shares
            BigDecimal dayProfit = currentPrice.subtract(prevPrice).multiply(shares);

            // 3. 累计盈亏 = (currentPrice - cost) * shares
            BigDecimal totalProfitItem = currentPrice.subtract(cost).multiply(shares);

            // 4. 持有收益率 = (currentPrice - cost) / cost * 100
            BigDecimal profitRate = BigDecimal.ZERO;
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                profitRate = currentPrice.subtract(cost).divide(cost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }

            totalMarketValue = totalMarketValue.add(marketValue);
            totalDayProfit = totalDayProfit.add(dayProfit);
            totalProfit = totalProfit.add(totalProfitItem);

            // 统计今日涨跌数量（基于今日盈亏）
            if (dayProfit.compareTo(BigDecimal.ZERO) > 0) {
                todayUpCount++;  // 今日上涨
            } else if (dayProfit.compareTo(BigDecimal.ZERO) < 0) {
                todayDownCount++;  // 今日下跌
            }
            // 注意：如果 dayProfit == 0，既不算上涨也不算下跌

            // 生成当前请求时间（HH:mm:ss格式）
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 构建 VO）
            FundItemVO fundItemVO = new FundItemVO();
            fundItemVO.setCode(code);
            fundItemVO.setName(name != null ? name : "未知");
            fundItemVO.setShares(shares);
            fundItemVO.setCost(cost);
            fundItemVO.setCurrentPrice(currentPrice);
            fundItemVO.setPrevPrice(prevPrice);
            fundItemVO.setChangePercent(changePercent.setScale(2, RoundingMode.HALF_UP));
            fundItemVO.setMarketValue(marketValue.setScale(2, RoundingMode.HALF_UP));
            fundItemVO.setDayProfit(dayProfit.setScale(2, RoundingMode.HALF_UP));
            fundItemVO.setTotalProfit(totalProfitItem.setScale(2, RoundingMode.HALF_UP));
            fundItemVO.setProfitRate(profitRate.setScale(2, RoundingMode.HALF_UP));
            fundItemVO.setUpdateTime(currentTime);

            fundItemList.add(fundItemVO);
        }

        // 计算今日总收益率 = 今日总盈亏 / (总市值 - 今日总盈亏) * 100
        // 昨日总市值 = 总市值 - 今日总盈亏
        BigDecimal totalDayProfitRate = BigDecimal.ZERO;
        BigDecimal yesterdayTotalMarketValue = totalMarketValue.subtract(totalDayProfit);
        if (yesterdayTotalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            totalDayProfitRate = totalDayProfit.divide(yesterdayTotalMarketValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 计算总持有收益率 = 累计总盈亏 / 总成本 * 100
        // 总成本 = 总市值 - 累计总盈亏
        BigDecimal totalProfitRate = BigDecimal.ZERO;
        BigDecimal totalCost = totalMarketValue.subtract(totalProfit);
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitRate = totalProfit.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        result.setFundList(fundItemList);
        result.setTotalMarketValue(totalMarketValue.setScale(2, RoundingMode.HALF_UP));
        result.setTotalDayProfit(totalDayProfit.setScale(2, RoundingMode.HALF_UP));
        result.setTotalDayProfitRate(totalDayProfitRate.setScale(2, RoundingMode.HALF_UP));
        result.setTotalProfit(totalProfit.setScale(2, RoundingMode.HALF_UP));
        result.setTotalProfitRate(totalProfitRate.setScale(2, RoundingMode.HALF_UP));
        result.setTodayUpCount(todayUpCount);
        result.setTodayDownCount(todayDownCount);

        return result;
    }

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

    @Override
    public List<MarketIndexVO> getMajorIndices() {
        List<MarketIndexVO> result = new ArrayList<>();

        for (String[] index : MAJOR_INDICES) {
            String code = index[0];
            String defaultName = index[1];

            try {
                // 调用数据源获取指数数据
                JSONObject indexData = fundDataService.fetchIndexData(code);

                if (indexData != null && !indexData.isEmpty()) {
                    MarketIndexVO vo = new MarketIndexVO();
                    vo.setIndexCode(code);
                    vo.setIndexName(indexData.getString("name") != null ? indexData.getString("name") : defaultName);
                    vo.setCurrentValue(BigDecimal.valueOf(indexData.getDoubleValue("current")).setScale(2, RoundingMode.HALF_UP));
                    vo.setChangeValue(BigDecimal.valueOf(indexData.getDoubleValue("change")).setScale(2, RoundingMode.HALF_UP));

                    // 格式化涨跌幅，带上%符号
                    BigDecimal changePct = BigDecimal.valueOf(indexData.getDoubleValue("changePct")).setScale(2, RoundingMode.HALF_UP);
                    vo.setChangePercent(changePct.toPlainString() + "%");

                    result.add(vo);
                } else {
                    log.warn("获取指数数据失败，使用默认值 - 指数: {}", defaultName);
                    // 失败时也添加一个默认VO，避免前端显示不完整
                    MarketIndexVO vo = new MarketIndexVO();
                    vo.setIndexCode(code);
                    vo.setIndexName(defaultName);
                    vo.setCurrentValue(BigDecimal.ZERO);
                    vo.setChangeValue(BigDecimal.ZERO);
                    vo.setChangePercent("0.00%");
                    result.add(vo);
                }
            } catch (Exception e) {
                log.error("处理指数数据异常 - 指数: {}, 错误: {}", defaultName, e.getMessage());
            }
        }

        return result;
    }
}




