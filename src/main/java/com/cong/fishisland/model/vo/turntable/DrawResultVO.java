package com.cong.fishisland.model.vo.turntable;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 抽奖结果视图对象
 * @author cong
 */
@Data
public class DrawResultVO implements Serializable {
    /**
     * 抽奖获得的物品列表
     */
    private List<DrawPrizeVO> prizeList;

    /**
     * 是否触发保底
     */
    private Boolean isGuarantee;

    /**
     * 保底类型：1-小保底 2-大保底
     */
    private Integer guaranteeType;

    /**
     * 消耗积分
     */
    private Integer costPoints;

    private static final long serialVersionUID = 1L;
}
