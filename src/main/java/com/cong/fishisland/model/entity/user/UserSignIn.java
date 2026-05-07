package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户签到记录
 *
 * @TableName user_sign_in
 */
@TableName(value = "user_sign_in")
@Data
public class UserSignIn {

    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 签到日期
     */
    @TableField(value = "signDate")
    private Date signDate;

    /**
     * 签到类型：1-正常签到，2-补签
     */
    @TableField(value = "signType")
    private Integer signType;

    /**
     * 当次签到后的连续天数
     */
    @TableField(value = "continuousDays")
    private Integer continuousDays;

    /**
     * 本次签到获得的积分奖励（含连续奖励）
     */
    @TableField(value = "rewardPoints")
    private Integer rewardPoints;

    /**
     * 创建时间
     */
    @TableField(value = "createTime")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updateTime")
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableField(value = "isDelete")
    private Integer isDelete;
}
