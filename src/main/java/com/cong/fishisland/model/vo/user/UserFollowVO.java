package com.cong.fishisland.model.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注/粉丝用户信息 VO
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@ApiModel(value = "UserFollowVO", description = "关注/粉丝用户信息")
public class UserFollowVO implements Serializable {

    /**
     * 用户 ID（字符串，防止前端 Long 精度丢失）
     */
    @ApiModelProperty(value = "用户ID")
    private String userId;

    /**
     * 用户昵称
     */
    @ApiModelProperty(value = "用户昵称")
    private String userName;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "用户头像")
    private String userAvatar;

    /**
     * 用户简介
     */
    @ApiModelProperty(value = "用户简介")
    private String userProfile;

    /**
     * 用户头像框地址
     */
    @ApiModelProperty(value = "用户头像框地址")
    private String avatarFramerUrl;

    /**
     * 是否已互相关注（互粉）
     */
    @ApiModelProperty(value = "是否互相关注")
    private Boolean isMutual;

    /**
     * 关注时间
     */
    @ApiModelProperty(value = "关注时间")
    private Date followTime;

    private static final long serialVersionUID = 1L;
}
