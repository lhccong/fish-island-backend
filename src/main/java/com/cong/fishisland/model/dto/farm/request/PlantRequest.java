package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "PlantRequest", description = "种植作物请求")
public class PlantRequest {

    @ApiModelProperty(value = "农场用户ID（可不传，以当前登录用户为准）")
    private Long userId;

    @ApiModelProperty(value = "地块ID", required = true)
    private Long landId;

    @ApiModelProperty(value = "作物ID", required = true)
    private Long cropId;
}
