package com.cong.fishisland.model.entity.pet;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 宠物自动喂食配置实体
 *
 * @TableName pet_auto_feed_config
 */
@TableName(value = "pet_auto_feed_config")
@Data
public class PetAutoFeedConfig implements Serializable {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "userId")
    private Long userId;

    /**
     * 宠物ID
     */
    @TableField(value = "petId")
    private Long petId;

    /**
     * 是否启用：0-关闭，1-开启
     */
    @TableField(value = "enabled")
    private Integer enabled;

    /**
     * 使用的食物模板code，关联 item_templates.code
     */
    @TableField(value = "foodCode")
    private String foodCode;

    /**
     * 触发喂食的饱食度阈值（低于此值时自动喂食）
     * hunger 越高越饱，低于此阈值说明宠物饿了需要喂食
     */
    @TableField(value = "triggerThreshold")
    private Integer triggerThreshold;

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
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
