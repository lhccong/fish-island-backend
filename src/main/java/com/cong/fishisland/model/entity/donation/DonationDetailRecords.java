package com.cong.fishisland.model.entity.donation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 打赏明细记录表（每次打赏独立一条，不累加）
 *
 * @TableName donation_detail_records
 */
@TableName(value = "donation_detail_records")
@Data
public class DonationDetailRecords implements Serializable {

    /**
     * 明细记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 打赏用户ID
     */
    private Long userId;

    /**
     * 本次打赏金额（元）
     */
    private BigDecimal amount;

    /**
     * 打赏留言/备注
     */
    private String remark;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 打赏时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
