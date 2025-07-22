package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.PointConstant;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.mapper.user.UserPointsMapper;
import com.cong.fishisland.utils.RedisUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

/**
 * @author cong
 * @description 针对表【user_points(用户积分)】的数据库操作Service实现
 * @createDate 2025-03-12 16:13:45
 */
@Service
public class UserPointsServiceImpl extends ServiceImpl<UserPointsMapper, UserPoints>
        implements UserPointsService {

    private static final String SIGN_IN_KEY_PREFIX = "user:signin:";
    private static final String SPEAK_KEY_PREFIX = "user:speak:";
    private static final int MAX_DAILY_SPEAK_POINTS = 20;


    @Override
    public boolean signIn() {
        Object loginUserId = StpUtil.getLoginId();

        String signKey = SIGN_IN_KEY_PREFIX + loginUserId + ":" + LocalDate.now();

        // 使用 SETNX 实现原子性判断和设置
        // **存入 Redis，避免重复签到**
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDayMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Duration expireDuration = Duration.between(now, nextDayMidnight);
        Boolean success = RedisUtils.setIfAbsent(signKey, "1", expireDuration);

        if (!success) {
            // 说明已经签到
            return false;
        }

        // **数据库更新积分**
        updatePoints(Long.valueOf(loginUserId.toString()), PointConstant.SIGN_IN_POINT, true);


        return true;
    }

    @Override
    public void updatePoints(Long userId, Integer points, boolean isSignIn) {
        UserPoints userPoints = this.getById(userId);
        userPoints.setPoints(userPoints.getPoints() + points);
        //积分除以 100去整计算等级
        userPoints.setLevel(calculateLevel(userPoints.getPoints()));
        if (isSignIn) {
            userPoints.setLastSignInDate(new Date());
        }
        this.updateById(userPoints);
    }

    @Override
    public void updateUsedPoints(Long userId, Integer points) {
        UserPoints userPoints = this.getById(userId);
        userPoints.setUsedPoints(userPoints.getPoints() == null ? points : userPoints.getUsedPoints() + points);

        this.updateById(userPoints);
    }

    public int calculateLevel(int points) {
        // 等级对应的积分范围 (起始积分)
        int[] thresholds = {0, 125, 300, 600, 1100, 2100, 4100, 6000, 8000, 10000, 12000, 14000, 16000, 18000};

        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (points >= thresholds[i]) {
                // 级别从 1 开始
                return i + 1;
            }
        }
        // 默认返回 1 级（防止异常情况）
        return 1;
    }

    @Override
    public void addSpeakPoint(Long userId) {
        // **发言积分**
        String speakKey = SPEAK_KEY_PREFIX + userId + ":" + LocalDate.now();

        // 获取当前用户今日的发言积分总数
        Integer currentSpeakPoints = Optional.ofNullable(RedisUtils.get(speakKey))
                .map(Integer::parseInt)
                .orElse(0);

        if (currentSpeakPoints >= MAX_DAILY_SPEAK_POINTS) {
            // 超过每日上限
            return;
        }

        // **数据库增加积分**
        updatePoints(userId, PointConstant.SPEAK_POINT, false);

        // **更新 Redis 计数**
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDayMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Duration expireDuration = Duration.between(now, nextDayMidnight);
        //增加发言积分
        RedisUtils.inc(speakKey, expireDuration);

    }

    /**
     * 扣除积分
     *
     * @param userId         用户ID
     * @param pointsToDeduct 要扣除的积分
     */
    @Override
    public void deductPoints(Long userId, Integer pointsToDeduct) {
        // 检查用户积分是否足够
        UserPoints userPoints = this.getById(userId);
        ThrowUtils.throwIf(userPoints == null, ErrorCode.NOT_FOUND_ERROR, "用户积分不存在");
        int availablePoints = userPoints.getPoints() - userPoints.getUsedPoints();
        ThrowUtils.throwIf(availablePoints < pointsToDeduct, ErrorCode.OPERATION_ERROR, "用户积分不足");
        userPoints.setUsedPoints(userPoints.getUsedPoints() + pointsToDeduct);
        this.updateById(userPoints);
    }

}




