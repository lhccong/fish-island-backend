package com.cong.fishisland.model.dto.fund;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 指数交易记录查询请求
 *
 * @author shing
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class IndexTransactionQueryRequest extends PageRequest implements Serializable {

    /**
     * 指数代码
     */
    private String indexCode;

    /**
     * 交易类型：1-买入，2-卖出
     */
    private Integer tradeType;

    /**
     * 状态：0-待结算，1-已完成
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
