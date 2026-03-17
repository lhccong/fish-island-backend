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
 * 基金持仓项 VO
 * 用于返回单个基金的持仓信息和实时估值数据
 *
 * @author shing
 */
@Data
@ApiModel(description = "基金持仓项")
public class FundItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== 基础信息 ====================

    @ApiModelProperty(value = "基金代码", example = "013478")
    private String code;

    @ApiModelProperty(value = "基金名称", example = "华宝中证金融科技主题ETF发起式联接C")
    private String name;

    // ==================== 持仓数据（保持原始精度）====================

    @ApiModelProperty(value = "持有份额", example = "42.11")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal shares;

    @ApiModelProperty(value = "成本价", example = "1.1902")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal cost;

    // ==================== 实时行情（保持原始精度）====================

    @ApiModelProperty(value = "当前价格（实时估值）", example = "1.1124")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal currentPrice;

    @ApiModelProperty(value = "昨日净值", example = "1.1107")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal prevPrice;

    // ==================== 计算数据（格式化为两位小数）====================

    @ApiModelProperty(value = "涨跌幅（%）", example = "0.15")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal changePercent;

    @ApiModelProperty(value = "持有市值", example = "46.84")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal marketValue;

    @ApiModelProperty(value = "今日盈亏", example = "0.07")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal dayProfit;

    @ApiModelProperty(value = "累计盈亏", example = "-3.28")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal totalProfit;

    @ApiModelProperty(value = "持有收益率（%）", example = "-6.54")
    @JsonSerialize(using = BigDecimalTwoDecimalSerializer.class)
    private BigDecimal profitRate;

    // ==================== 其他信息 ====================

    @ApiModelProperty(value = "更新时间", example = "19:40:15")
    private String updateTime;
}