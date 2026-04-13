package com.cong.fishisland.model.vo.fund;

import com.cong.fishisland.common.BigDecimalTwoDecimalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 指数持仓 VO
 * 用于返回用户的指数持仓信息
 *
 * @author shing
 */
@Data
@ApiModel(description = "指数持仓信息")
public class IndexPositionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "指数代码", example = "sh000001")
    private String indexCode;

    @ApiModelProperty(value = "指数名称", example = "上证指数")
    private String indexName;

    @ApiModelProperty(value = "总份额", example = "1000.5000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal totalShares;

    @ApiModelProperty(value = "可用份额（可卖出）", example = "800.5000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal availableShares;

    @ApiModelProperty(value = "锁定份额（当日买入，次日解锁）", example = "200.0000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal lockedShares;

    @ApiModelProperty(value = "冻结份额（卖出待结算）", example = "200.0000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal frozenShares;

    @ApiModelProperty(value = "平均成本（净值）", example = "3.1500")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal avgCost;

    @ApiModelProperty(value = "当前净值", example = "3.2500")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal currentNav;

    @ApiModelProperty(value = "涨跌幅（%）", example = "1.25")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal changePercent;

    @ApiModelProperty(value = "持仓市值（积分）", example = "3250.00")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal marketValue;

    @ApiModelProperty(value = "累计盈亏（积分）", example = "100.00")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalProfit;

    @ApiModelProperty(value = "持有收益率（%）", example = "3.17")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal profitRate;

    @ApiModelProperty(value = "更新时间", example = "2026-04-07 15:00:00")
    private String updateTime;
}
