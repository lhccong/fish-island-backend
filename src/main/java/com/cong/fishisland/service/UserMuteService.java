package com.cong.fishisland.service;

import com.cong.fishisland.model.vo.user.UserMuteVO;

/**
 * 用户禁言服务
 *
 * @author cong
 */
public interface UserMuteService {

    /**
     * 禁言用户
     *
     * @param userId   用户ID
     * @param duration 禁言时长（秒）
     * @return 是否成功
     */
    boolean muteUser(Long userId, Long duration);

    /**
     * 解除用户禁言
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean unmuteUser(Long userId);

    /**
     * 获取用户禁言状态
     *
     * @param userId 用户ID
     * @return 用户禁言信息
     */
    UserMuteVO getUserMuteInfo(Long userId);
} 