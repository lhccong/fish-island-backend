package com.cong.fishisland.model.dto.item;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Shing
 * date 27/9/2025 星期六
 */
@Data
@ApiModel("物品实例更新请求")
public class ItemInstanceUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "物品实例ID", required = true)
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
     * 数量
     */
    @ApiModelProperty(value = "数量")
    private Integer quantity;

    /**
     * 绑定状态：1-绑定，0-未绑定
     */
    @ApiModelProperty(value = "绑定状态：1-绑定，0-未绑定")
    private Integer bound;

    /**
     * 强化等级
     */
    @ApiModelProperty(value = "强化等级")
    private Integer enhanceLevel;

    /**
     * 耐久度
     */
    @ApiModelProperty(value = "扩展信息(JSON)")
    private String extraData;

}
