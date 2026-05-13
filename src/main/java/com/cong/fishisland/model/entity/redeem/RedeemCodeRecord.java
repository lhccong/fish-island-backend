package com.cong.fishisland.model.entity.redeem;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 兑换码使用记录表
 *
 * @author cong
 */
@TableName(value = "redeem_code_record")
@Data
public class RedeemCodeRecord implements Serializable {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兑换码ID
     */
    private Long codeId;

    /**
     * 兑换码（冗余）
     */
    private String code;

    /**
     * 使用者用户ID
     */
    private Long userId;

    /**
     * 奖励类型（冗余快照）
     */
    private Integer rewardType;

    /**
     * 奖励值（冗余快照）
     */
    private Long rewardValue;

    /**
     * 奖励数量（冗余快照）
     */
    private Integer rewardCount;

    /**
     * 兑换时间
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
