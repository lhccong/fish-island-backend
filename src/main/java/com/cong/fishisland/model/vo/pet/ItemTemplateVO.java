package com.cong.fishisland.model.vo.pet;

import lombok.Data;

import java.io.Serializable;

/**
 * 物品模板视图对象
 * @author Shing
 * date 26/9/2025 星期五
 */
@Data
public class ItemTemplateVO implements Serializable {


    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板唯一码，例如 sword_iron_01
     */
    private String code;

    /**
     * 物品名称
     */
    private String name;

    /**
     * 物品大类：equipment-装备类、consumable-消耗品、material-材料
     */
    private String category;

    /**
     * 物品子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套
     */
    private String subType;

    /**
     * 可穿戴槽位
     */
    private String equipSlot;

    /**
     * 稀有度等级（1-8）
     */
    private Integer rarity;

    /**
     * 使用等级需求
     */
    private Integer levelReq;

    /**
     * 基础攻击力
     */
    private Integer baseAttack;

    /**
     * 基础防御力
     */
    private Integer baseDefense;

    /**
     * 基础生命值
     */
    private Integer baseHp;

    /**
     * 物品图标地址
     */
    private String icon;

    /**
     * 物品描述
     */
    private String description;

    /**
     * 是否可叠加，0-不可叠加，1-可叠加
     */
    private Integer stackable;

    /**
     * 分解后获得的积分
     */
    private Integer removePoint;
}
