package com.cong.fishisland.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户积分记录
 * @TableName user_points_record
 */
@TableName(value = "user_points_record")
@Data
public class UserPointsRecord {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 变动类型：1-增加，2-扣除
     */
    @TableField(value = "change_type")
    private Integer changeType;

    /**
     * 变动积分数量
     */
    @TableField(value = "change_points")
    private Integer changePoints;

    /**
     * 变动前总积分
     */
    @TableField(value = "before_points")
    private Integer beforePoints;

    /**
     * 变动后总积分
     */
    @TableField(value = "after_points")
    private Integer afterPoints;

    /**
     * 变动前已用积分
     */
    @TableField(value = "before_used_points")
    private Integer beforeUsedPoints;

    /**
     * 变动后已用积分
     */
    @TableField(value = "after_used_points")
    private Integer afterUsedPoints;

    /**
     * 来源类型
     */
    @TableField(value = "source_type")
    private String sourceType;

    /**
     * 来源ID
     */
    @TableField(value = "source_id")
    private String sourceId;

    /**
     * 描述/备注
     */
    @TableField(value = "description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;
}
