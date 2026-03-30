package com.cong.fishisland.model.vo.turntable;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户抽奖进度视图对象
 * @author cong
 */
@Data
public class UserProgressVO implements Serializable {
    /**
     * 小保底失败次数
     */
    private Integer smallFailCount;

    /**
     * 累计抽奖次数
     */
    private Integer totalDrawCount;

    /**
     * 保底阈值
     */
    private Integer guaranteeCount;

    /**
     * 上次抽奖时间
     */
    private Date lastDrawTime;

    private static final long serialVersionUID = 1L;
}
