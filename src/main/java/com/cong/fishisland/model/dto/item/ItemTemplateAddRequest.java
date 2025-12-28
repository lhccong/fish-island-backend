package com.cong.fishisland.model.dto.item;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加物品模板请求
 * 用于新增物品模板时的请求参数对象，字段与 {@link com.cong.fishisland.model.entity.pet.ItemTemplates} 对应。
 *
 * @author Shing
 * date 27/9/2025 星期六
 */
@Data
//@ApiModel("添加物品模板请求")
public class ItemTemplateAddRequest implements Serializable {
    /**
     * 模板唯一码，例如 sword_iron_01
     */
    @ApiModelProperty(value = "模板唯一码，例如 sword_iron_01")
    private String code;

    /**
     * 物品名称
     */
    @ApiModelProperty(value = "物品名称")
    private String name;

    /**
     * 物品大类：equipment-装备类（能穿戴的）、consumable-消耗品（药水/卷轴/食物）、material-材料（强化石/合成材料）
     */
    @ApiModelProperty(value = "物品大类：equipment-装备类（能穿戴的）、consumable-消耗品（药水/卷轴/食物）、material-材料（强化石/合成材料）")
    private String category;

    /**
     * 物品子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套
     */
    @ApiModelProperty(value = "物品子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套")
    private String subType;

    /**
     * 可穿戴槽位: head-头部, hand-手部, foot-脚部, weapon-武器；NULL 表示无法穿戴
     */
    @ApiModelProperty(value = "可穿戴槽位: head-头部, hand-手部, foot-脚部, weapon-武器；NULL 表示无法穿戴")
    private String equipSlot;

    /**
     * 稀有度等级（1-8，数字越高越稀有）
     */
    @ApiModelProperty(value = "稀有度等级（1-8，数字越高越稀有）")
    private Integer rarity;

    /**
     * 使用等级需求
     */
    @ApiModelProperty(value = "使用等级需求")
    private Integer levelReq;

    /**
     * 基础攻击力
     */
    @ApiModelProperty(value = "基础攻击力")
    private Integer baseAttack;

    /**
     * 基础防御力
     */
    @ApiModelProperty(value = "基础防御力")
    private Integer baseDefense;

    /**
     * 基础生命值
     */
    @ApiModelProperty(value = "基础生命值")
    private Integer baseHp;

    /**
     * 非常规属性/词缀(JSON)，格式: [{k,v},...]
     */
    @ApiModelProperty(value = "非常规属性/词缀(JSON)，格式: [{k,v},...]")
    private Object mainAttr;

    /**
     * 物品图标地址
     */
    @ApiModelProperty(value = "物品图标地址")
    private String icon;

    /**
     * 物品描述
     */
    @ApiModelProperty(value = "物品描述")
    private String description;

    /**
     * 是否可叠加，0-不可叠加，1-可叠加（如消耗品）
     */
    @ApiModelProperty(value = "是否可叠加，0-不可叠加，1-可叠加（如消耗品）")
    private Integer stackable;

    /**
     * 分解后获得的积分
     */
    @ApiModelProperty(value = "分解后获得的积分")
    private Integer removePoint;

    private static final long serialVersionUID = 1L;
}