package com.cong.fishisland.model.entity.pet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 物品模板表（通用配置，包括装备、消耗品、材料等）
 *
 * @TableName item_templates
 */
@TableName(value = "item_templates")
@Data
public class ItemTemplates implements Serializable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模板唯一码，例如 sword_iron_01
     */
    @TableField(value = "code")
    private String code;

    /**
     * 物品名称
     */
    @TableField(value = "name")
    private String name;

    /**
     * 物品大类：equipment-装备类（能穿戴的）、consumable-消耗品（药水/卷轴/食物）、material-材料（强化石/合成材料）
     */
    @TableField(value = "category")
    private String category;

    /**
     * 物品子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套
     */
    @TableField(value = "sub_type")
    private String subType;

    /**
     * 可穿戴槽位: head-头部, hand-手部, foot-脚部, weapon-武器；NULL 表示无法穿戴
     */
    @TableField(value = "equip_slot")
    private String equipSlot;

    /**
     * 稀有度等级（1-8，数字越高越稀有）
     */
    @TableField(value = "rarity")
    private Integer rarity;

    /**
     * 使用等级需求
     */
    @TableField(value = "levelReq")
    private Integer levelReq;

    /**
     * 基础攻击力
     */
    @TableField(value = "baseAttack")
    private Integer baseAttack;

    /**
     * 基础防御力
     */
    @TableField(value = "baseDefense")
    private Integer baseDefense;

    /**
     * 基础生命值
     */
    @TableField(value = "baseHp")
    private Integer baseHp;

    /**
     * 非常规属性/词缀(JSON)，格式: [{k,v},...]
     */
    @TableField(value = "mainAttr")
    private String mainAttr;

    /**
     * 物品图标地址
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 物品描述
     */
    @TableField(value = "description")
    private String description;

    /**
     * 是否可叠加，0-不可叠加，1-可叠加（如消耗品）
     */
    @TableField(value = "stackable")
    private Integer stackable;

    /**
     * 分解后获得的积分
     */
    @TableField(value = "removePoint")
    private Integer removePoint;

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
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}