package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "StealRequest", description = "偷菜请求")
public class StealRequest {

    @ApiModelProperty(value = "地块ID", required = true)
    private Long landId;
}
