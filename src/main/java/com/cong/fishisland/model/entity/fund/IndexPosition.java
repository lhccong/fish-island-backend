package com.cong.fishisland.model.entity.fund;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 指数持仓
 * totalShares = availableShares + lockedShares
 *
 * @TableName index_position
 */
@TableName(value = "index_position")
@Data
public class IndexPosition implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "userId")
    private Long userId;

    @TableField(value = "indexCode")
    private String indexCode;

    /** 总份额 = availableShares + lockedShares */
    @TableField(value = "totalShares")
    private BigDecimal totalShares;

    /** 可用份额（可卖出） */
    @TableField(value = "availableShares")
    private BigDecimal availableShares;

    /** 锁定份额（当日买入，次日解锁） */
    @TableField(value = "lockedShares")
    private BigDecimal lockedShares;

    /** 平均成本（净值） */
    @TableField(value = "avgCost")
    private BigDecimal avgCost;

    @TableField(value = "updateTime")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
