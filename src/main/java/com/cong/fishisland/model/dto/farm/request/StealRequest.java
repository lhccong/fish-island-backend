package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "偷菜请求")
public class StealRequest {

    @ApiModelProperty(value = "种植记录ID", required = true)
    private Long plantRecordId;
}
