package com.cong.fishisland.model.ws.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author cong
 */
@Data
public class UserChatResponse {

    @ApiModelProperty(value = "用户 ID")
    private String id;

    @ApiModelProperty(value = "用户名称")
    private String name;

    @ApiModelProperty(value = "用户头像")
    private String avatar;

    @ApiModelProperty(value = "用户等级")
    private Integer level;

    @ApiModelProperty(value = "头像框 URL")
    private String avatarFramerUrl;

    @ApiModelProperty(value = "用户简介")
    private String userProfile;

    @ApiModelProperty(value = "用户称号 ID")
    private Long titleId;

    @ApiModelProperty(value = "用户称号ID列表")
    private String titleIdList;

    @ApiModelProperty(value = "用户积分")
    private Integer points;

    @ApiModelProperty(value = "是否是管理员")
    private Boolean isAdmin;

    @ApiModelProperty(value = "用户状态")
    private String status;
}
