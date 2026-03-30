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
 * 转盘奖励表
 * @author cong
 */
@TableName(value = "turntable_prize")
@Data
public class TurntablePrize implements Serializable {
    /**
     * 奖励ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 转盘ID
     */
    private Long turntableId;

    /**
     * 物品ID
     */
    private Long prizeId;

    /**
     * 奖励品质 1-普通(N) 2-稀有(R) 3-史诗(SR) 4-传说(SSR)
     */
    private Integer quality;

    /**
     * 物品类型 1-装备 2-称号
     */
    private Integer prizeType;

    /**
     * 概率权重，总权重1000
     */
    private Integer probability;

    /**
     * 奖品数量 -1表示无限
     */
    private Integer stock;

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
