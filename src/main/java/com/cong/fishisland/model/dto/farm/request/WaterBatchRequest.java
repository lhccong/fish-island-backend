package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
@ApiModel(description = "批量浇水请求")
public class WaterBatchRequest {
    @ApiModelProperty(value = "地块ID列表，最多24个，不可重复", required = true)
    private List<Long> landIds;
}
