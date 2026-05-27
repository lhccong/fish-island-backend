package com.cong.fishisland.model.dto.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 指数买入请求
 *
 * @author shing
 */
@Data
public class IndexBuyRequest implements Serializable {

    /**
     * 指数代码（sh000001/sz399001/sz399006/sh000300/sh000016，默认 sh000001）
     */
    @ApiModelProperty(value = "指数代码", example = "sh000001")
    private String indexCode;

    @ApiModelProperty(value = "买入金额（积分）", required = true)
    private Long amount;

    private static final long serialVersionUID = 1L;
}
