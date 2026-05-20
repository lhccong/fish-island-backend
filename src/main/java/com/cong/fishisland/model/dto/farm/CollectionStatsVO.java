package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 收集册统计 VO
 */
@Data
@ApiModel(description = "收集册统计VO")
public class CollectionStatsVO {

    @ApiModelProperty(value = "已获得作物数量")
    private Long obtained;

    @ApiModelProperty(value = "作物总数")
    private Long total;

    @ApiModelProperty(value = "收集进度（百分比，0-100）")
    private Long progress;
}
