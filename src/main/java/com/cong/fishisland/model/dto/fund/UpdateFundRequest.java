package com.cong.fishisland.model.dto.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员更新基金请求
 *
 * @author shing
 */
@Data
public class UpdateFundRequest {

    @ApiModelProperty(value = "用户ID", required = true)
    private Long userId;

    @ApiModelProperty(value = "基金代码", required = true)
    private String code;

    @ApiModelProperty(value = "基金名称")
    private String name;

    @ApiModelProperty(value = "持有份额")
    private BigDecimal shares;

    @ApiModelProperty(value = "成本净值")
    private BigDecimal cost;
}