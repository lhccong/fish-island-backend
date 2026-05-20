package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "排行榜DTO")
public class RankingDTO {

    @ApiModelProperty(value = "排名")
    private Integer rank;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "今日数值")
    private Integer todayValue;

    @ApiModelProperty(value = "总数值")
    private Integer totalValue;
}
