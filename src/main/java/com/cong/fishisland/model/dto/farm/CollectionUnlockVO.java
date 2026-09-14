package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收获时新解锁的图鉴条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "收获新解锁图鉴")
public class CollectionUnlockVO {

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "作物名称")
    private String cropName;

    @ApiModelProperty(value = "作物图标")
    private String cropIcon;

    @ApiModelProperty(value = "品级 1-10")
    private Integer grade;

    @ApiModelProperty(value = "重量（克）")
    private Integer weight;

    @ApiModelProperty(value = "首次解锁时间")
    private LocalDateTime firstObtainedTime;

    @ApiModelProperty(value = "图鉴更新额外奖励积分")
    private Integer bonus;
}
