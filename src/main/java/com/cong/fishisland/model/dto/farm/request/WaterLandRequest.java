package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "农场浇水请求")
public class WaterLandRequest {

    @ApiModelProperty(value = "地块ID", required = true)
    private Long landId;
}
