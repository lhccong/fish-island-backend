package com.cong.fishisland.model.dto.luckybag;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 创建福袋请求
 */
@Data
public class CreateLuckyBagRequest {

    @ApiModelProperty(value = "福袋名称", example = "祝大家摸鱼快乐")
    private String name;

    @ApiModelProperty(value = "福袋总积分（1-100，且需满足单人最多50积分）", required = true, example = "50")
    private Integer totalAmount;

    @ApiModelProperty(value = "中奖人数（需满足总积分÷人数向上取整≤50）", required = true, example = "5")
    private Integer winnerCount;

    @ApiModelProperty(value = "分配类型：1-随机，2-平均", required = true, example = "1")
    private Integer type;

    @ApiModelProperty(value = "持续秒数（60-1800，默认180）", example = "180")
    private Integer durationSeconds;
}
