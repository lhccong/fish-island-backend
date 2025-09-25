package com.cong.fishisland.service.impl.user;

import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.user.UserMuteVO;
import com.cong.fishisland.service.UserMuteService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Objects;

/**
 * 用户禁言服务实现
 *
 * @author cong
 */
@Service
@Slf4j
public class UserMuteServiceImpl implements UserMuteService {

    @Resource
    private UserService userService;

    @Override
    public boolean muteUser(Long userId, Long duration) {

        User loginUser = userService.getLoginUser();

        if (userId == null || duration == null || duration <= 0 || !Objects.equals(loginUser.getUserRole(), UserConstant.ADMIN_ROLE)) {
            return false;
        }

        try {
            String muteKey = RedisKey.getKey(RedisKey.USER_MUTE, userId);
            // 设置禁言，值为结束时间的时间戳
            RedisUtils.set(muteKey, String.valueOf(System.currentTimeMillis() + (duration * 1000)), Duration.ofSeconds(duration));
            return true;
        } catch (Exception e) {
            log.error("禁言用户失败，userId={}, duration={}", userId, duration, e);
            return false;
        }
    }

    @Override
    public boolean unmuteUser(Long userId) {

        User loginUser = userService.getLoginUser();

        if (userId == null || !Objects.equals(loginUser.getUserRole(), UserConstant.ADMIN_ROLE)) {
            return false;
        }

        try {
            String muteKey = RedisKey.getKey(RedisKey.USER_MUTE, userId);
            // 检查是否存在禁言记录
            if (!RedisUtils.hasKey(muteKey)) {
                // 用户本来就没有被禁言
                return true;
            }
            // 直接删除禁言记录
            return RedisUtils.delete(muteKey);
        } catch (Exception e) {
            log.error("解除用户禁言失败，userId={}", userId, e);
            return false;
        }
    }

    @Override
    public UserMuteVO getUserMuteInfo(Long userId) {
        UserMuteVO vo = new UserMuteVO();
        vo.setIsMuted(false);
        vo.setRemainingTime("");

        if (userId == null) {
            return vo;
        }

        try {
            String muteKey = RedisKey.getKey(RedisKey.USER_MUTE, userId);
            // 检查是否存在禁言记录
            if (!RedisUtils.hasKey(muteKey)) {
                // 用户未被禁言
                return vo;
            }

            // 获取禁言结束时间
            String endTimeStr = RedisUtils.get(muteKey);
            if (endTimeStr == null || endTimeStr.isEmpty()) {
                // 禁言已结束
                return vo;
            }

            long endTime = Long.parseLong(endTimeStr);
            long currentTime = System.currentTimeMillis();
            long remainingMillis = endTime - currentTime;

            if (remainingMillis <= 0) {
                // 禁言已结束，但Redis的key尚未过期
                return vo;
            }

            // 计算剩余时间（时分秒）
            long seconds = remainingMillis / 1000;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long remainingSeconds = seconds % 60;

            // 构建更直白的禁言信息
            StringBuilder messageBuilder = new StringBuilder("您已被禁言，剩余时间");
            if (hours > 0) {
                messageBuilder.append(hours).append("小时");
            }
            if (minutes > 0) {
                messageBuilder.append(minutes).append("分钟");
            }
            if (remainingSeconds > 0 || (hours == 0 && minutes == 0)) {
                messageBuilder.append(remainingSeconds).append("秒");
            }

            vo.setIsMuted(true);
            vo.setRemainingTime(messageBuilder.toString());

            return vo;
        } catch (Exception e) {
            log.error("获取用户禁言信息失败，userId={}", userId, e);
            return vo;
        }
    }
} 