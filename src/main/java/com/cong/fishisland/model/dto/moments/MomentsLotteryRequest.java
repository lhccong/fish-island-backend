package com.cong.fishisland.model.dto.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 朋友圈抽奖请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsLotteryRequest", description = "朋友圈抽奖请求")
public class MomentsLotteryRequest implements Serializable {

    @ApiModelProperty(value = "动态ID", required = true)
    private Long momentId;

    @ApiModelProperty(value = "抽奖人数（1-100）", required = true, example = "3")
    private Integer winnerCount;

    private static final long serialVersionUID = 1L;
}
