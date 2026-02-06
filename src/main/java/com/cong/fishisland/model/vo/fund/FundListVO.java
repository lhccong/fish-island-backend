package com.cong.fishisland.model.vo.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 基金持仓列表 VO
 *
 * @author shing
 */
@Data
public class FundListVO {

    @ApiModelProperty("基金列表")
    private List<FundItemVO> fundList;

    @ApiModelProperty("总市值")
    private BigDecimal totalMarketValue;

    @ApiModelProperty("今日总盈亏")
    private BigDecimal totalDayProfit;

    @ApiModelProperty("累计总盈亏")
    private BigDecimal totalProfit;

    @ApiModelProperty("今日上涨的基金数量")
    private Integer todayUpCount;

    @ApiModelProperty("今日下跌的基金数量")
    private Integer todayDownCount;
}
