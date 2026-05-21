package com.cong.fishisland.model.vo.luckybag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 福袋中奖记录 VO
 */
@Data
@ApiModel(value = "福袋中奖记录VO")
public class LuckyBagRecordVO {

    @ApiModelProperty(value = "记录ID")
    private String id;

    @ApiModelProperty(value = "福袋ID")
    private String luckyBagId;

    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @ApiModelProperty(value = "用户昵称")
    private String userName;

    @ApiModelProperty(value = "用户头像")
    private String userAvatar;

    @ApiModelProperty(value = "中奖积分")
    private Integer amount;

    @ApiModelProperty(value = "中奖时间")
    private Date winTime;
}
