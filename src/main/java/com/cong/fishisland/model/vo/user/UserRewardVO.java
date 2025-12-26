package com.cong.fishisland.model.vo.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户兑奖视图（包含加密字段）
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class UserRewardVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 兑奖加密token（用户id + 盐值加密）
     */
    private String rewardToken;

    private static final long serialVersionUID = 1L;
}




