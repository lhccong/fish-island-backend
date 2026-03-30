package com.cong.fishisland.model.entity.turntable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 转盘抽奖记录表
 * @author cong
 */
@TableName(value = "turntable_draw_record")
@Data
public class TurntableDrawRecord implements Serializable {
    /**
     * 抽奖记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 转盘ID
     */
    private Long turntableId;

    /**
     * 转盘奖励ID
     */
    private Long turntablePrizeId;

    /**
     * 奖品名称
     */
    private String name;

    /**
     * 奖励类型 1-装备 2-称号
     */
    private Integer prizeType;

    /**
     * 奖励ID
     */
    private Long prizeId;

    /**
     * 本次消耗积分
     */
    private Integer costPoints;

    /**
     * 是否触发保底 0-否 1-是
     */
    private Integer isGuarantee;

    /**
     * 保底类型 1-小保底 2-大保底
     */
    private Integer guaranteeType;

    /**
     * 奖励品质 1-普通(N) 2-稀有(R) 3-史诗(SR) 4-传说(SSR)
     */
    private Integer quality;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
