package com.cong.fishisland.model.vo.fund;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指数交易记录 VO
 * 用于返回用户的交易记录详情
 *
 * @author shing
 */
@Data
@ApiModel(description = "指数交易记录")
public class IndexTransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "交易记录ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty(value = "指数代码", example = "sh000001")
    private String indexCode;

    @ApiModelProperty(value = "指数名称", example = "上证指数")
    private String indexName;

    @ApiModelProperty(value = "交易类型：1-买入，2-卖出", example = "1")
    private Integer tradeType;

    @ApiModelProperty(value = "交易类型名称", example = "买入")
    private String tradeTypeName;

    @ApiModelProperty(value = "交易金额（积分）", example = "1000")
    private Long amount;

    @ApiModelProperty(value = "成交净值", example = "3.1500")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal nav;

    @ApiModelProperty(value = "成交份额", example = "317.4603")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal shares;

    @ApiModelProperty(value = "状态：0-待结算，1-已完成", example = "1")
    private Integer status;

    @ApiModelProperty(value = "状态名称", example = "已完成")
    private String statusName;

    @ApiModelProperty(value = "盈亏金额（积分，仅卖出）", example = "50")
    private Long profitLoss;

    @ApiModelProperty(value = "预计结算日期（仅卖出）", example = "2026-04-08")
    private LocalDate expectedSettleDate;

    @ApiModelProperty(value = "实际结算时间（仅卖出）", example = "2026-04-08 09:30:00")
    private LocalDateTime actualSettleTime;

    @ApiModelProperty(value = "下单时间", example = "2026-04-07 14:30:00")
    private LocalDateTime createTime;
}
