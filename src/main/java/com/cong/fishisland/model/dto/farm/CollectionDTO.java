package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "收集册DTO")
public class CollectionDTO {

    @ApiModelProperty(value = "收集记录ID")
    private Long id;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;

    @ApiModelProperty(value = "作物名称")
    private String cropName;

    @ApiModelProperty(value = "作物分类")
    private String category;

    @ApiModelProperty(value = "是否已获得（0-未获得，1-已获得）")
    private Integer obtained;

    @ApiModelProperty(value = "收集次数")
    private Integer count;

    @ApiModelProperty(value = "首次获得时间")
    private LocalDateTime obtainedTime;
}
