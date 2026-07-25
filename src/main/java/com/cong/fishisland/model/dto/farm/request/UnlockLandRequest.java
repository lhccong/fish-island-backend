package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "UnlockLandRequest", description = "解锁农场地块请求")
public class UnlockLandRequest {

    @ApiModelProperty(value = "地块ID", required = true)
    private Long landId;
}
