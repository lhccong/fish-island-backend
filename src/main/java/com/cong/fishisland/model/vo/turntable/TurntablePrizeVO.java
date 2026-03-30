package com.cong.fishisland.model.vo.turntable;

import lombok.Data;

import java.io.Serializable;

/**
 * 转盘奖励视图对象
 * @author cong
 */
@Data
public class TurntablePrizeVO implements Serializable {
    /**
     * 奖励ID
     */
    private Long id;

    /**
     * 物品ID
     */
    private Long prizeId;

    /**
     * 奖励品质 1-普通(N) 2-稀有(R) 3-史诗(SR) 4-传说(SSR)
     */
    private Integer quality;

    /**
     * 品质名称
     */
    private String qualityName;

    /**
     * 物品类型 1-装备 2-称号
     */
    private Integer prizeType;

    /**
     * 物品名称
     */
    private String name;

    /**
     * 物品图片
     */
    private String icon;

    /**
     * 概率权重
     */
    private Integer probability;

    private static final long serialVersionUID = 1L;
}
