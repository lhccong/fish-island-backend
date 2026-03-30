package com.cong.fishisland.model.vo.turntable;

import lombok.Data;

import java.io.Serializable;

/**
 * 抽奖奖品视图对象
 * @author cong
 */
@Data
public class DrawPrizeVO implements Serializable {
    /**
     * 奖品ID
     */
    private Long prizeId;

    /**
     * 转盘奖励ID
     */
    private Long turntablePrizeId;

    /**
     * 奖品名称
     */
    private String name;

    /**
     * 奖品图片
     */
    private String icon;

    /**
     * 奖品品质 1-普通(N) 2-稀有(R) 3-史诗(SR) 4-传说(SSR)
     */
    private Integer quality;

    /**
     * 品质名称
     */
    private String qualityName;

    /**
     * 奖品类型 1-装备 2-称号
     */
    private Integer prizeType;

    /**
     * 是否已置换为积分
     */
    private Boolean convertedToPoints;

    /**
     * 置换积分数目
     */
    private Integer convertedPoints;

    private static final long serialVersionUID = 1L;
}
