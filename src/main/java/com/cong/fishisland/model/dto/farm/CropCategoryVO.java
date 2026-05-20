package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "作物分类选项")
public class CropCategoryVO {

    @ApiModelProperty(value = "分类编码（grain/vegetable/fruit/flower）")
    private String value;

    @ApiModelProperty(value = "分类名称")
    private String label;
}
