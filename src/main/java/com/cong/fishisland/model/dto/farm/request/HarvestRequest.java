package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "HarvestRequest", description = "批量收获作物请求")
public class HarvestRequest {

    @ApiModelProperty(value = "农场用户ID（可不传，以当前登录用户为准）")
    private Long userId;

    @ApiModelProperty(value = "地块ID列表", required = true)
    private List<Long> landIds;
}
