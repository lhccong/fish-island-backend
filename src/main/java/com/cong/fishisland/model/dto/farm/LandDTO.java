package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "农场地块DTO")
public class LandDTO {

    @ApiModelProperty(value = "地块ID")
    private Long id;

    @ApiModelProperty(value = "地块索引")
    private Integer landIndex;

    @ApiModelProperty(value = "地块状态（0-空闲，1-种植中，2-已成熟）")
    private Integer status;

    @ApiModelProperty(value = "种植的作物ID")
    private Long plantedCropId;

    @ApiModelProperty(value = "作物名称")
    private String cropName;

    @ApiModelProperty(value = "种植时间")
    private LocalDateTime plantedTime;

    @ApiModelProperty(value = "收获时间")
    private LocalDateTime harvestTime;

    @ApiModelProperty(value = "是否锁定（0-未锁定，1-已锁定）")
    private Integer locked;

    @ApiModelProperty(value = "解锁所需农场等级；超出当前可解锁范围时为 null")
    private Integer unlockLevel;

    @ApiModelProperty(value = "解锁所需可用积分；默认已解锁或超出当前可解锁范围时为 null")
    private Integer unlockCost;

    @ApiModelProperty(value = "是否可以偷菜（false 表示已偷过、未成熟或无可偷积分）")
    private Boolean canSteal;
}
