package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "StealRequest", description = "偷菜请求")
public class StealRequest {

    @ApiModelProperty(value = "地块ID（与 landIds 二选一）")
    private Long landId;

    @ApiModelProperty(value = "地块ID列表（批量偷菜，与 landId 二选一）")
    private List<Long> landIds;
}
