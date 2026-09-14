package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农场成就任务 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "农场成就任务")
public class FarmAchievementTaskVO {

    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    @ApiModelProperty(value = "成就名称")
    private String name;

    @ApiModelProperty(value = "成就描述")
    private String description;

    @ApiModelProperty(value = "类型：buff_upgrade/buff_purchase/collection_unlock/farm_level")
    private String type;

    @ApiModelProperty(value = "目标值（等级/条目数/品级）")
    private Integer targetCount;

    @ApiModelProperty(value = "奖励数值")
    private Integer rewardValue;

    @ApiModelProperty(value = "状态：0-进行中 1-已完成未领取 2-已领取")
    private Integer status;

    @ApiModelProperty(value = "达成时间")
    private LocalDateTime completedTime;
}
