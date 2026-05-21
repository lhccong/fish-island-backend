package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "PlantItem", description = "单块地块种植项")
public class PlantItem {

    @ApiModelProperty(value = "地块ID", required = true)
    private Long landId;

    @ApiModelProperty(value = "作物ID", required = true)
    private Long cropId;
}
