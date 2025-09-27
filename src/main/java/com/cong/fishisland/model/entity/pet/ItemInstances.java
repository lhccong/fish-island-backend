package com.cong.fishisland.model.entity.pet;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 物品实例表（玩家真正持有的物品，每个实例可有强化、耐久、附魔等个性化信息）
 *
 * @TableName item_instances
 */
@TableName(value = "item_instances")
@Data
public class ItemInstances implements Serializable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 物品模板ID（关联 item_templates.id）
     */
    @TableField(value = "templateId")
    private Long templateId;

    /**
     * 持有者用户ID
     */
    @TableField(value = "ownerUserId")
    private Long ownerUserId;

    /**
     * 物品数量：若模板可叠加则 quantity>1，否则为1
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 是否绑定（1-绑定后不可交易，0-未绑定可交易）
     */
    @TableField(value = "bound")
    private Integer bound;

    /**
     * 耐久度（可选，部分装备适用）
     */
    @TableField(value = "durability")
    private Integer durability;

    /**
     * 强化等级
     */
    @TableField(value = "enhanceLevel")
    private Integer enhanceLevel;

    /**
     * 扩展信息（如附魔、镶嵌孔、特殊属性等JSON数据）
     */
    @TableField(value = "extraData")
    private transient Object extraData;

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
     * 是否删除，0-正常，1-已删除
     */
    @TableField(value = "isDelete")
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}