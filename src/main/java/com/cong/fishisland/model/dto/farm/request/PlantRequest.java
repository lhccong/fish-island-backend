package com.cong.fishisland.model.dto.farm.request;

import com.cong.fishisland.model.dto.farm.PlantItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "PlantRequest", description = "批量种植作物请求")
public class PlantRequest {

    @ApiModelProperty(value = "种植项列表（地块ID + 作物ID）", required = true)
    private List<PlantItem> items;
}
