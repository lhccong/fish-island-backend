package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(description = "访问好友农场VO")
public class FarmFriendFarmVO {

    @ApiModelProperty(value = "好友ID")
    private Long friendId;

    @ApiModelProperty(value = "好友昵称")
    private String friendName;

    @ApiModelProperty(value = "好友头像")
    private String friendAvatar;

    @ApiModelProperty(value = "地块列表")
    private List<LandDTO> lands;

    @ApiModelProperty(value = "是否可以偷菜")
    private Boolean canSteal;

    @ApiModelProperty(value = "最后访问时间")
    private LocalDateTime lastVisitTime;

    @ApiModelProperty(value = "偷菜冷却剩余时间（分钟）")
    private Integer stealCooldownMinutes;
}
