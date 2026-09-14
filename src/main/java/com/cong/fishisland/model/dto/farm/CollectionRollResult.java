package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图鉴 roll 结果：收获时随机获得品级/重量，供收获汇总展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "图鉴 roll 结果")
public class CollectionRollResult {

    @ApiModelProperty(value = "是否解锁了新图鉴（该作物该品级首条记录）")
    private boolean newEntry;

    @ApiModelProperty(value = "图鉴记录是否被更新（新建或最大重量被刷新）")
    private boolean updated;

    @ApiModelProperty(value = "品级 1-10")
    private Integer grade;

    @ApiModelProperty(value = "本次重量（克）")
    private Integer weight;

    @ApiModelProperty(value = "作物ID")
    private Long cropId;
}
