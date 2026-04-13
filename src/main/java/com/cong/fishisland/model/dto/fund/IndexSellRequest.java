package com.cong.fishisland.model.dto.fund;

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
     * 指数代码
     */
    private String indexCode;

    /**
     * 卖出份额
     */
    private BigDecimal shares;

    private static final long serialVersionUID = 1L;
}
