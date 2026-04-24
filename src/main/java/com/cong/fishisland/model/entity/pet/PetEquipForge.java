package com.cong.fishisland.model.entity.pet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 宠物装备锻造实体
 * 每个宠物每个装备位置唯一（petId + equipSlot 联合唯一）
 *
 * @author cong
 */
@TableName(value = "pet_equip_forge", autoResultMap = true)
@Data
public class PetEquipForge implements Serializable {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 宠物ID
     */
    private Long petId;

    /**
     * 装备位置 1-武器 2-手套 3-鞋子 4-头盔 5-项链 6-翅膀
     */
    private Integer equipSlot;

    /**
     * 装备等级（默认0）
     */
    private Integer equipLevel;

    /**
     * 词条1
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private EquipEntry entry1;

    /**
     * 词条2
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private EquipEntry entry2;

    /**
     * 词条3
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private EquipEntry entry3;

    /**
     * 词条4
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private EquipEntry entry4;

    /**
     * 创建时间
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

    private static final long serialVersionUID = 1L;
}
