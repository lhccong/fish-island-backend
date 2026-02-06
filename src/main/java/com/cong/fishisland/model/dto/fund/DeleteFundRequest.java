package com.cong.fishisland.model.dto.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 删除基金请求
 *
 * @author shing
 */
@Data
public class DeleteFundRequest {

    @ApiModelProperty(value = "基金代码", required = true)
    private String code;
}
