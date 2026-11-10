package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "单株浇水处理结果")
public class WaterLandResult {
    private Long landId;
    private boolean success;
    private String message;
    @ApiModelProperty(value = "成功时返回更新后的地块")
    private LandDTO land;
}
