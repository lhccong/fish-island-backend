package com.cong.fishisland.model.dto.item;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 编辑物品模板请求
 *
 * @author Shing
 * date 28/9/2025 星期日
 */
@Data
//@ApiModel("编辑物品模板请求（管理员专用）")
public class ItemTemplateEditRequest implements Serializable {

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "物品模板ID", required = true, example = "1001")
    private Long id;

    /**
     * 模板唯一码，例如 sword_iron_01
     */
    @ApiModelProperty(value = "模板唯一码", required = true, example = "sword_admin")
    private String code;

    /**
     * 物品名称
     */
    @ApiModelProperty(value = "物品名称", required = true, example = "铁剑")
    private String name;

    /**
     * 物品大类：equipment-装备类、consumable-消耗品、material-材料
     */
    @ApiModelProperty(value = "物品大类", required = true, example = "equipment")
    private String category;

    /**
     * 物品子类型，例如 weapon 武器、head 头盔、foot 鞋子
     */
    @ApiModelProperty(value = "物品子类型", example = "weapon")
    private String subType;

    /**
     * 可穿戴槽位: head-头部, hand-手部, foot-脚部, weapon-武器；NULL 表示无法穿戴
     */
    @ApiModelProperty(value = "可穿戴槽位", example = "weapon")
    private String equipSlot;

    /**
     * 稀有度等级（1-8，数字越高越稀有）
     */
    @ApiModelProperty(value = "稀有度等级（1-8）", example = "5")
    private Integer rarity;

    /**
     * 使用等级需求
     */
    @ApiModelProperty(value = "使用等级需求", example = "30")
    private Integer levelReq;

    /**
     * 基础攻击力
     */
    @ApiModelProperty(value = "基础攻击力", example = "30")
    private Integer baseAttack;

    /**
     * 基础防御力
     */
    @ApiModelProperty(value = "基础防御力", example = "12")
    private Integer baseDefense;

    /**
     * 基础生命值
     */
    @ApiModelProperty(value = "基础生命值", example = "200")
    private Integer baseHp;

    /**
     * 非常规属性/词缀(JSON)，格式: [{k,v},...]
     */
    @ApiModelProperty(value = "非常规属性/词缀(JSON)")
    private Object mainAttr;

    /**
     * 物品图标地址
     */
    @ApiModelProperty(value = "物品图标地址", example = "http://cdn.xx.com/icon/sword_admin.png")
    private String icon;

    /**
     * 物品描述
     */
    @ApiModelProperty(value = "物品描述", example = "管理员修改的高级铁剑")
    private String description;

    /**
     * 是否可叠加，0-不可叠加，1-可叠加
     */
    @ApiModelProperty(value = "是否可叠加（0-不可叠加，1-可叠加）", example = "0")
    private Integer stackable;

    /**
     * 分解后获得的积分
     */
    @ApiModelProperty(value = "分解后获得的积分", example = "30")
    private Integer removePoint;

    /**
     * 逻辑删除标识（0-正常，1-已删除）
     */
    @ApiModelProperty(value = "逻辑删除标识（0-正常，1-已删除）", example = "0")
    private Integer isDelete;


    private static final long serialVersionUID = 1L;
}
