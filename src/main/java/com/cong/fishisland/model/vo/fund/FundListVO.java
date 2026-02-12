package com.cong.fishisland.model.vo.fund;

import com.cong.fishisland.common.BigDecimalTwoDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 基金持仓列表 VO
 * 用于返回用户的基金持仓列表及汇总数据
 *
 * @author shing
 */
@Data
@ApiModel(description = "基金持仓列表")
public class FundListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基金列表 ====================

    @ApiModelProperty(value = "基金列表", required = true)
    private List<FundItemVO> fundList;

    // ==================== 汇总数据（格式化为两位小数）====================

    @ApiModelProperty(value = "总市值", example = "1349.92")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalMarketValue;

    @ApiModelProperty(value = "今日总盈亏", example = "7.70")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalDayProfit;

    @ApiModelProperty(value = "今日总收益率（%）", example = "0.57")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalDayProfitRate;

    @ApiModelProperty(value = "累计总盈亏", example = "-114.05")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalProfit;

    @ApiModelProperty(value = "总持有收益率（%）", example = "-7.79")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalProfitRate;

    // ==================== 统计数据 ====================

    @ApiModelProperty(value = "今日上涨的基金数量", example = "7")
    private Integer todayUpCount;

    @ApiModelProperty(value = "今日下跌的基金数量", example = "1")
    private Integer todayDownCount;
}