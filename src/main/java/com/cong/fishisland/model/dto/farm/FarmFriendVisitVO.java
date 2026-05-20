package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "好友访问信息VO")
public class FarmFriendVisitVO {

    @ApiModelProperty(value = "好友ID")
    private Long friendId;

    @ApiModelProperty(value = "最后访问时间")
    private LocalDateTime lastVisitTime;

    @ApiModelProperty(value = "是否可以偷菜")
    private Boolean canSteal;
}
