package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.*;
import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.model.enums.VipTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户会员表
 *
 * @TableName user_vip
 */
@TableName(value = "user_vip")
@Data
public class UserVip implements Serializable {

    /**
     * 会员ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会员兑换卡号（永久会员无卡号）
     */
    private String cardNo;

    /**
     * 会员类型
     * {@link VipTypeEnum}
     * {@link VipTypeConstant#MONTHLY} - 月卡会员
     * {@link VipTypeConstant#PERMANENT} - 永久会员
     */
    private Integer type;

    /**
     * 会员到期时间，永久会员为null
     */
    private Date validDays;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
} 