package com.cong.fishisland.model.vo.turntable;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 转盘视图对象
 * @author cong
 */
@Data
public class TurntableVO implements Serializable {
    /**
     * 转盘ID
     */
    private Long id;

    /**
     * 转盘类型 1-宠物装备转盘 2-称号转盘
     */
    private Integer type;

    /**
     * 转盘名称
     */
    private String name;

    /**
     * 每次抽奖消耗积分
     */
    private Integer costPoints;

    /**
     * 保底触发次数
     */
    private Integer guaranteeCount;

    /**
     * 用户抽奖进度
     */
    private UserProgressVO userProgress;

    /**
     * 转盘奖励列表
     */
    private List<TurntablePrizeVO> prizeList;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
