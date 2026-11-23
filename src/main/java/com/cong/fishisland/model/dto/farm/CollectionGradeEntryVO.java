package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作物图鉴条目 VO：某作物某品级的唯一记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "作物图鉴条目")
public class CollectionGradeEntryVO {

    @ApiModelProperty(value = "品级 1-10")
    private Integer grade;

    @ApiModelProperty(value = "品级图标")
    private String gradeIcon;

    @ApiModelProperty(value = "该品级最大重量（克）")
    private Integer maxWeight;

    @ApiModelProperty(value = "首次解锁该品级时间")
    private LocalDateTime firstObtainedTime;
}
