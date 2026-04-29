package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.PointConstant;
import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.mapper.user.UserVipMapper;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.service.UserPointsRecordService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.mapper.user.UserPointsMapper;
import com.cong.fishisland.service.UserVipService;
import com.cong.fishisland.utils.RedisUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.*;

/**
 * @author cong
 * @description 针对表【user_points(用户积分)】的数据库操作Service实现
 * @createDate 2025-03-12 16:13:45
 */
@Service
public class UserPointsServiceImpl extends ServiceImpl<UserPointsMapper, UserPoints>
        implements UserPointsService {
    @Resource
    private UserVipMapper userVipMapper;

    @Resource
    private UserPointsRecordService userPointsRecordService;

    private static final String SIGN_IN_KEY_PREFIX = "user:signin:";
    private static final String SPEAK_KEY_PREFIX = "user:speak:";
    private static final int MAX_DAILY_SPEAK_POINTS = 10;


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
        Long userId = Long.valueOf(loginUserId.toString());
        updatePoints(userId, PointConstant.SIGN_IN_POINT, true);

        // 记录积分变动
        UserPoints userPoints = this.getById(userId);
        int beforePoints = userPoints.getPoints() - PointConstant.SIGN_IN_POINT;
        int afterPoints = userPoints.getPoints();
        int usedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
        userPointsRecordService.addPointsIncreaseRecord(userId, PointConstant.SIGN_IN_POINT, SIGN_IN.getValue(), "每日签到奖励",
                beforePoints, afterPoints, usedPoints, usedPoints);

        if (isUserVip(userId)) {
            updateUsedPoints(userId, -PointConstant.SIGN_IN_POINT);
            // VIP签到返还积分记录
            UserPoints vipUserPoints = this.getById(userId);
            int vipBeforePoints = vipUserPoints.getPoints() - PointConstant.SIGN_IN_POINT;
            int vipAfterPoints = vipUserPoints.getPoints();
            int vipBeforeUsedPoints = vipUserPoints.getUsedPoints() + PointConstant.SIGN_IN_POINT;
            int vipAfterUsedPoints = vipUserPoints.getUsedPoints();
            userPointsRecordService.addPointsIncreaseRecord(userId, PointConstant.SIGN_IN_POINT, SIGN_IN.getValue(), "VIP签到积分返还",
                    vipBeforePoints, vipAfterPoints, vipBeforeUsedPoints, vipAfterUsedPoints);
        }


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

//    @Override
//    public void addPoints(Long userId, Integer points, String sourceType, String sourceId, String description) {
//        UserPoints userPoints = this.getById(userId);
//        int beforePoints = userPoints.getPoints();
//        int afterPoints = beforePoints + points;
//        userPoints.setPoints(afterPoints);
//        userPoints.setLevel(calculateLevel(afterPoints));
//        this.updateById(userPoints);
//
//        int usedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
//        userPointsRecordService.addPointsIncreaseRecord(userId, points, sourceType, description,
//                beforePoints, afterPoints, usedPoints, usedPoints);
//    }

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

        // 记录积分变动
        UserPoints speakUserPoints = this.getById(userId);
        int speakBeforePoints = speakUserPoints.getPoints() - PointConstant.SPEAK_POINT;
        int speakAfterPoints = speakUserPoints.getPoints();
        int speakUsedPoints = speakUserPoints.getUsedPoints() == null ? 0 : speakUserPoints.getUsedPoints();
        userPointsRecordService.addPointsIncreaseRecord(userId, PointConstant.SPEAK_POINT, SPEAK.getValue(), "房间发言奖励",
                speakBeforePoints, speakAfterPoints, speakUsedPoints, speakUsedPoints);

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
        int beforeUsedPoints = userPoints.getUsedPoints();
        userPoints.setUsedPoints(userPoints.getUsedPoints() + pointsToDeduct);
        this.updateById(userPoints);

        // 记录积分变动
        userPointsRecordService.addPointsRecord(userId, 2, pointsToDeduct,
                userPoints.getPoints(), userPoints.getPoints(),
                beforeUsedPoints, userPoints.getUsedPoints(),
                OTHER.getValue(), null, "积分扣除");
    }

    /**
     * 扣除积分（带来源信息）
     *
     * @param userId         用户ID
     * @param pointsToDeduct 要扣除的积分
     * @param sourceType     来源类型
     * @param sourceId       来源ID
     * @param description    描述
     */
    public void deductPoints(Long userId, Integer pointsToDeduct, String sourceType, String sourceId, String description) {
        // 检查用户积分是否足够
        UserPoints userPoints = this.getById(userId);
        ThrowUtils.throwIf(userPoints == null, ErrorCode.NOT_FOUND_ERROR, "用户积分不存在");
        int availablePoints = userPoints.getPoints() - userPoints.getUsedPoints();

        ThrowUtils.throwIf(availablePoints < pointsToDeduct, ErrorCode.OPERATION_ERROR, "用户积分不足");
        int beforeUsedPoints = userPoints.getUsedPoints();

        userPoints.setUsedPoints(userPoints.getUsedPoints() + pointsToDeduct);
        this.updateById(userPoints);

        // 记录积分变动
        userPointsRecordService.addPointsRecord(userId, 2, pointsToDeduct,
                userPoints.getPoints(), userPoints.getPoints(),
                beforeUsedPoints, userPoints.getUsedPoints(),
                sourceType, sourceId, description);
    }

    /**
     * 更新已用积分（带来源信息）
     *
     * @param userId      用户ID
     * @param points      积分变动（负数表示返还）
     * @param sourceType  来源类型
     * @param sourceId    来源ID
     * @param description 描述
     */
    public void updateUsedPoints(Long userId, Integer points, String sourceType, String sourceId, String description) {
        UserPoints userPoints = this.getById(userId);
        int beforeUsedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
        int afterUsedPoints = beforeUsedPoints + points;
        userPoints.setUsedPoints(afterUsedPoints);
        this.updateById(userPoints);

        // 记录积分变动
        if (points < 0) {
            // 积分返还
            userPointsRecordService.addPointsRecord(userId, 1, -points,
                    userPoints.getPoints(), userPoints.getPoints(),
                    beforeUsedPoints, afterUsedPoints,
                    sourceType, sourceId, description);
        } else {
            // 积分扣除
            userPointsRecordService.addPointsRecord(userId, 2, points,
                    userPoints.getPoints(), userPoints.getPoints(),
                    beforeUsedPoints, afterUsedPoints,
                    sourceType, sourceId, description);
        }
    }

    @Override
    public void checkAvailablePoints(Long userId, Integer requiredPoints) {
        UserPoints userPoints = this.getById(userId);
        ThrowUtils.throwIf(userPoints == null, ErrorCode.NOT_FOUND_ERROR, "积分信息不存在");
        int total = userPoints.getPoints() == null ? 0 : userPoints.getPoints();
        int used = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
        ThrowUtils.throwIf(total - used < requiredPoints, ErrorCode.OPERATION_ERROR, "积分不足");
    }

    public boolean isUserVip(Long userId) {
        if (userId == null) {
            return false;
        }

        // 查询用户会员信息
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        UserVip userVip = userVipMapper.selectOne(queryWrapper);

        if (userVip == null) {
            return false;
        }

        // 如果是永久会员，直接返回true
        if (VipTypeConstant.PERMANENT.equals(userVip.getType())) {
            return true;
        }

        // 如果是月卡会员，检查是否过期
        Date now = new Date();
        return userVip.getValidDays() != null && now.before(userVip.getValidDays());
    }

}




