package com.cong.fishisland.model.vo.fund;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 市场指数VO（仅包含前端展示需要的数据）
 *
 * @author shing
 */
@Data
public class MarketIndexVO implements Serializable {

    /**
     * 指数代码
     */
    private String indexCode;

    /**
     * 指数名称
     */
    private String indexName;

    /**
     * 当前点位
     */
    private BigDecimal currentValue;

    /**
     * 涨跌点数
     */
    private BigDecimal changeValue;

    /**
     * 涨跌幅度
     */
    private String changePercent;

    private static final long serialVersionUID = 1L;
}
