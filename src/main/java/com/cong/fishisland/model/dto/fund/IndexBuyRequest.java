package com.cong.fishisland.model.dto.fund;

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
     * 指数代码
     */
    private String indexCode;

    /**
     * 买入金额（积分）
     */
    private Long amount;

    private static final long serialVersionUID = 1L;
}
