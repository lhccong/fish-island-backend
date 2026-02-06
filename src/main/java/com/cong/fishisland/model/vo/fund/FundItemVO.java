package com.cong.fishisland.model.vo.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 基金持仓项 VO
 *
 * @author shing
 */
@Data
public class FundItemVO {

    @ApiModelProperty("基金代码")
    private String code;

    @ApiModelProperty("基金名称")
    private String name;

    @ApiModelProperty("持有份额")
    private BigDecimal shares;

    @ApiModelProperty("成本价")
    private BigDecimal cost;

    @ApiModelProperty("当前价格（实时估值）")
    private BigDecimal currentPrice;

    @ApiModelProperty("昨日净值")
    private BigDecimal prevPrice;

    @ApiModelProperty("涨跌幅（%）")
    private BigDecimal changePercent;

    @ApiModelProperty("持有市值")
    private BigDecimal marketValue;

    @ApiModelProperty("今日盈亏")
    private BigDecimal dayProfit;

    @ApiModelProperty("累计盈亏")
    private BigDecimal totalProfit;

    @ApiModelProperty("更新时间")
    private String updateTime;
}
