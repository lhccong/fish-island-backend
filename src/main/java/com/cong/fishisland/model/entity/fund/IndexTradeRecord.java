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
 * 指数交易记录表（买入/卖出及T+1结算）
 * @TableName index_trade_record
 */
@TableName(value ="index_trade_record")
@Data
public class IndexTradeRecord implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联user表
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 指数代码
     */
    @TableField(value = "indexCode")
    private String indexCode;

    /**
     * 交易类型：1-买入，2-卖出
     */
    @TableField(value = "tradeType")
    private Integer tradeType;

    /**
     * 交易金额（积分）
     */
    @TableField(value = "amount")
    private Long amount;

    /**
     * 成交时的指数净值
     */
    @TableField(value = "nav")
    private BigDecimal nav;

    /**
     * 成交份额
     */
    @TableField(value = "shares")
    private BigDecimal shares;

    /**
     * 状态：0-待确认，1-已确认/已结算
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 预计结算日期（T+1）
     */
    @TableField(value = "expectedSettleDate")
    private Date expectedSettleDate;

    /**
     * 实际结算完成时间
     */
    @TableField(value = "actualSettleTime")
    private Date actualSettleTime;

    /**
     * 仅卖出有效：盈亏金额（积分）
     */
    @TableField(value = "profitLoss")
    private Long profitLoss;

    /**
     * 下单时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
