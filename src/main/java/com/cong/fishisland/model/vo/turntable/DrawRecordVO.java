package com.cong.fishisland.model.vo.turntable;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 抽奖记录视图对象
 * @author cong
 */
@Data
public class DrawRecordVO implements Serializable {
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 奖品名称
     */
    private String name;

    /**
     * 奖品图片
     */
    private String icon;

    /**
     * 奖品品质
     */
    private Integer quality;

    /**
     * 品质名称
     */
    private String qualityName;

    /**
     * 抽奖消耗积分
     */
    private Integer costPoints;

    /**
     * 是否触发保底
     */
    private Boolean isGuarantee;

    /**
     * 保底类型：1-小保底 2-大保底
     */
    private Integer guaranteeType;

    /**
     * 抽奖时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
