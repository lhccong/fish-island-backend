package com.cong.fishisland.model.dto.item;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Shing
 * date 27/9/2025 星期六
 */
@Data
//@ApiModel("物品实例编辑请求（管理员用）")
public class ItemInstanceEditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "物品实例ID", required = true)
    private Long id;

    @ApiModelProperty(value = "物品模板ID（允许修改实例关联的模板）")
    private Long templateId;

    @ApiModelProperty(value = "持有者用户ID（允许转移所有权）")
    private Long ownerUserId;

    @ApiModelProperty(value = "数量（大于0有效）")
    private Integer quantity;

    @ApiModelProperty(value = "是否绑定（1绑定，0未绑定）")
    private Integer bound;

    @ApiModelProperty(value = "耐久度")
    private Integer durability;

    @ApiModelProperty(value = "强化等级")
    private Integer enhanceLevel;

    @ApiModelProperty(value = "扩展信息（JSON字符串或对象）")
    private transient Object extraData;

    @ApiModelProperty(value = "是否需要返回模板信息，默认 true")
    private Boolean includeTemplate = true;
}
