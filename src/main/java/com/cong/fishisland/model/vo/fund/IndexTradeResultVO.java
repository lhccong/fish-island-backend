package com.cong.fishisland.model.vo.fund;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 指数交易结果 VO
 * 用于返回买入/卖出操作的结果
 *
 * @author shing
 */
@Data
@ApiModel(description = "指数交易结果")
public class IndexTradeResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "交易记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long transactionId;

    @ApiModelProperty(value = "交易类型：1-买入，2-卖出", example = "1")
    private Integer tradeType;

    @ApiModelProperty(value = "交易类型名称", example = "买入")
    private String tradeTypeName;

    @ApiModelProperty(value = "成交净值", example = "3.1500")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal nav;

    @ApiModelProperty(value = "成交份额", example = "317.4603")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal shares;

    @ApiModelProperty(value = "交易金额（积分）", example = "1000")
    private Long amount;

    @ApiModelProperty(value = "预计结算日期（仅卖出有效）", example = "2026-04-08")
    private LocalDate expectedSettleDate;

    @ApiModelProperty(value = "提示信息", example = "买入成功，份额已到账")
    private String message;
}
