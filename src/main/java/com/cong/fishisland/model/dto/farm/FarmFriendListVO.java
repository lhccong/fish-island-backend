package com.cong.fishisland.model.dto.farm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 农场互关好友列表 VO
 */
@Data
@ApiModel(description = "农场互关好友列表VO")
public class FarmFriendListVO {

    @ApiModelProperty(value = "好友系统用户ID")
    private Long friendId;

    @ApiModelProperty(value = "好友系统用户ID")
    private Long systemUserId;

    @ApiModelProperty(value = "好友昵称")
    private String nickname;

    @ApiModelProperty(value = "好友头像")
    private String avatar;

    @ApiModelProperty(value = "好友等级")
    private Integer level;

    @ApiModelProperty(value = "是否可以偷菜（存在至少一块您尚未偷过的可偷土地）")
    private Boolean canSteal;
}
