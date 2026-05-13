package com.cong.fishisland.model.dto.user;

import com.cong.fishisland.common.PageRequest;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 关注/粉丝列表分页请求
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "UserFollowPageRequest", description = "关注/粉丝列表分页请求")
public class UserFollowPageRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
}
