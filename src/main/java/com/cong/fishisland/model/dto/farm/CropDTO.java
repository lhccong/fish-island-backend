package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "作物DTO")
public class CropDTO {

    @ApiModelProperty(value = "作物ID")
    private Long id;

    @ApiModelProperty(value = "作物名称")
    private String name;

    @ApiModelProperty(value = "作物分类（粮食/蔬菜/水果/花卉/特产）")
    private String category;

    @ApiModelProperty(value = "生长时间（分钟）")
    private Integer growthTime;

    @ApiModelProperty(value = "收获经验")
    private Integer experience;

    @ApiModelProperty(value = "收获积分")
    private Integer coin;

    @ApiModelProperty(value = "稀有度")
    private Integer rarity;

    @ApiModelProperty(value = "作物图标")
    private String icon;

    @ApiModelProperty(value = "作物描述")
    private String description;
}
