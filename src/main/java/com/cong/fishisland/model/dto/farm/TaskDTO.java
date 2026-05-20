package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "任务DTO")
public class TaskDTO {

    @ApiModelProperty(value = "任务ID")
    private Long id;

    @ApiModelProperty(value = "任务名称")
    private String name;

    @ApiModelProperty(value = "任务描述")
    private String description;

    @ApiModelProperty(value = "目标次数")
    private Integer targetCount;

    @ApiModelProperty(value = "奖励经验")
    private Integer rewardExp;

    @ApiModelProperty(value = "任务类型")
    private String type;

    @ApiModelProperty(value = "当前进度次数")
    private Integer currentCount;

    @ApiModelProperty(value = "是否已完成（0-未完成，1-已完成）")
    private Integer completed;

    @ApiModelProperty(value = "是否已领取奖励（0-未领取，1-已领取）")
    private Integer claimed;
}
