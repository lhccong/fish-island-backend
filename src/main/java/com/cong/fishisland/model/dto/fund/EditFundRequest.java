package com.cong.fishisland.model.dto.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 编辑基金请求
 *
 * @author shing
 */
@Data
public class EditFundRequest {

    @ApiModelProperty(value = "基金代码", required = true)
    private String code;

    @ApiModelProperty(value = "持有金额", required = true)
    private BigDecimal amount;

    @ApiModelProperty(value = "盈亏金额（正数为盈利，负数为亏损）", required = true)
    private BigDecimal profit;
}