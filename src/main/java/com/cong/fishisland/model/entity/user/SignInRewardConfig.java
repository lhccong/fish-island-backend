package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 连续签到奖励配置
 *
 * @TableName sign_in_reward_config
 */
@TableName(value = "sign_in_reward_config")
@Data
public class SignInRewardConfig {

    /**
     * 配置ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 连续签到天数（达到该天数触发）
     */
    @TableField(value = "continuousDays")
    private Integer continuousDays;

    /**
     * 额外奖励积分（叠加在基础签到积分之上）
     */
    @TableField(value = "rewardPoints")
    private Integer rewardPoints;

    /**
     * 奖励描述
     */
    @TableField(value = "rewardDesc")
    private String rewardDesc;

    /**
     * 是否按周期循环：0-不循环，1-循环
     */
    @TableField(value = "isCycle")
    private Integer isCycle;

    /**
     * 循环周期天数（isCycle=1 时有效）
     */
    @TableField(value = "cycleDays")
    private Integer cycleDays;

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
