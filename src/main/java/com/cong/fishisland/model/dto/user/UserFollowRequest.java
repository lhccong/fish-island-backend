package com.cong.fishisland.model.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 关注/取消关注请求
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@ApiModel(value = "UserFollowRequest", description = "关注/取消关注请求")
public class UserFollowRequest implements Serializable {

    /**
     * 被关注用户 ID（字符串，防止前端 Long 精度丢失）
     */
    @ApiModelProperty(value = "被关注用户ID", required = true, example = "1893498757504978945")
    private String followUserId;

    private static final long serialVersionUID = 1L;
}
