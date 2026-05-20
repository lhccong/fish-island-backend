package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 农场好友列表VO
 * 包含好友基本信息和是否可以偷菜的标识
 */
@Data
@ApiModel(description = "农场好友列表VO")
public class FarmFriendListVO {

    @ApiModelProperty(value = "好友关系ID")
    private Long id;

    @ApiModelProperty(value = "好友ID")
    private Long friendId;

    @ApiModelProperty(value = "好友昵称")
    private String nickname;

    @ApiModelProperty(value = "好友头像")
    private String avatar;

    @ApiModelProperty(value = "好友等级")
    private Integer level;

    @ApiModelProperty(value = "好友关系状态（0-拉黑，1-正常）")
    private Integer status;

    @ApiModelProperty(value = "最后访问时间")
    private LocalDateTime lastVisitTime;

    @ApiModelProperty(value = "偷菜冷却时间")
    private LocalDateTime stealCooldown;

    @ApiModelProperty(value = "是否可以偷菜")
    private Boolean canSteal;
}
