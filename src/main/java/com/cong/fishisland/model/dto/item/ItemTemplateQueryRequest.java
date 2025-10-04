package com.cong.fishisland.model.dto.item;

import com.cong.fishisland.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 物品模板查询请求
 *
 * @author Shing
 * date 27/9/2025 星期六
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ItemTemplateQueryRequest extends PageRequest implements Serializable {

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "id")
    private Long id;

    /**
     * 模板唯一码（支持精确匹配）
     */
    @ApiModelProperty(value = "模板唯一码（精确匹配）", example = "sword_iron_01")
    private String code;

    /**
     * 物品名称（支持模糊搜索）
     */
    @ApiModelProperty(value = "物品名称（模糊搜索）", example = "铁剑")
    private String name;

    /**
     * 物品大类：equipment-装备类、consumable-消耗品、material-材料
     */
    @ApiModelProperty(value = "物品大类：equipment-装备类、consumable-消耗品、material-材料", example = "equipment")
    private String category;

    /**
     * 子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套
     */
    @ApiModelProperty(value = "子类型，例如 weapon 武器、head 头盔、foot 鞋子、hand 手套", example = "weapon")
    private String subType;

    /**
     * 是否可叠加（0-不可叠加，1-可叠加）
     */
    @ApiModelProperty(value = "是否可叠加（0-不可叠加，1-可叠加）", example = "1")
    private Integer stackable;

    private static final long serialVersionUID = 1L;

}
