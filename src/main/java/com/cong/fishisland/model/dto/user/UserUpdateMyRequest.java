package com.cong.fishisland.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户更新个人信息请求
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 朋友圈背景图
     */
    private String momentsBgUrl;

    private static final long serialVersionUID = 1L;
}