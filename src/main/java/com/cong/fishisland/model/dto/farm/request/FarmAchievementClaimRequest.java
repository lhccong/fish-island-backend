package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 农场成就奖励领取请求
 */
@Data
@ApiModel(description = "农场成就奖励领取请求")
public class FarmAchievementClaimRequest {

    @ApiModelProperty(value = "成就任务ID", required = true)
    private Long taskId;
}
