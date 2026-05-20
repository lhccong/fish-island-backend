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

    @ApiModelProperty(value = "是否可以偷菜")
    private Boolean canSteal;

    @ApiModelProperty(value = "种植记录ID（用于偷菜接口）")
    private Long plantRecordId;
}
