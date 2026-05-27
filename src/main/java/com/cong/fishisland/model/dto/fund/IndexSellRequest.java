package com.cong.fishisland.model.dto.fund;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 指数卖出请求
 *
 * @author shing
 */
@Data
public class IndexSellRequest implements Serializable {

    /**
     * 指数代码（sh000001/sz399001/sz399006/sh000300/sh000016，默认 sh000001）
     */
    @ApiModelProperty(value = "指数代码", example = "sh000001")
    private String indexCode;

    /**
     * 卖出份额
     */
    private BigDecimal shares;

    private static final long serialVersionUID = 1L;
}
