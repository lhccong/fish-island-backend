package com.cong.fishisland.model.dto.item;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加物品请求
 *
 * @author Shing
 * date 27/9/2025 星期六
 */
@Data
//@ApiModel("添加物品请求")
public class ItemInstanceAddRequest implements Serializable {

    /**
     * 物品模板ID
     */
    @ApiModelProperty(value = "物品模板ID", required = true)
    private Long templateId;

    /**
     * 持有者用户ID，如果不传默认添加当前登录用户
     */
    @ApiModelProperty(value = "持有者用户ID，如果不传默认添加当前登录用户")
    private Long ownerUserId;

    /**
     * 是否可叠加（1-可叠加，0-不可叠加）
     */
    @ApiModelProperty(value = "添加数量，stackable为1时有效，stackable为0会忽略")
    private Integer quantity = 1;

    /**
     * 是否绑定（1-绑定后不可交易，0-未绑定可交易）
     */
    @ApiModelProperty(value = "是否绑定（1-绑定后不可交易，0-未绑定可交易）")
    private Integer bound;

    /**
     * 耐久度（可选，部分装备适用）
     */
    @ApiModelProperty(value = "耐久度（可选，部分装备适用）")
    private Integer durability;

    /**
     * 强化等级
     */
    @ApiModelProperty(value = "强化等级")
    private Integer enhanceLevel;

    /**
     * 扩展信息（如附魔、镶嵌孔、特殊属性等JSON数据）
     */
    @ApiModelProperty(value = "扩展信息（如附魔、镶嵌孔、特殊属性等JSON数据）")
    private transient Object extraData;

    private static final long serialVersionUID = 1L;

}
