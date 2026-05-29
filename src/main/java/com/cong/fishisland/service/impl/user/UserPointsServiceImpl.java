package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.ActionTypeConstant;
import com.cong.fishisland.constant.PointConstant;
import com.cong.fishisland.constant.SourceTypeConstant;
import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.mapper.event.EventRemindMapper;
import com.cong.fishisland.mapper.user.UserVipMapper;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.model.vo.user.SignInVO;
import com.cong.fishisland.service.UserPointsRecordService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.mapper.user.UserPointsMapper;
import com.cong.fishisland.service.UserSignInService;
import com.cong.fishisland.utils.RedisUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    @Lazy
    @Resource
    private UserSignInService userSignInService;

    private static final String SIGN_IN_KEY_PREFIX = "user:signin:";
    private static final String SPEAK_KEY_PREFIX = "user:speak:";
    private static final String USER_POINTS_LOCK_PREFIX = "user:points:lock:";
    private static final int MAX_DAILY_SPEAK_POINTS = 10;
    private static final long POINTS_LOCK_WAIT_SECONDS = 5;
    private static final long POINTS_LOCK_LEASE_SECONDS = 10;
    /** 大额积分变动通知接收人（管理员） */
    private static final long POINTS_ALERT_ADMIN_USER_ID = 1L;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private EventRemindMapper eventRemindMapper;


    @Override
    public SignInVO signIn() {
        Object loginUserId = StpUtil.getLoginId();

        String signKey = SIGN_IN_KEY_PREFIX + loginUserId + ":" + LocalDate.now();

        // 使用 SETNX 实现原子性判断和设置，避免重复签到
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextDayMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Duration expireDuration = Duration.between(now, nextDayMidnight);
        Boolean success = RedisUtils.setIfAbsent(signKey, "1", expireDuration);

        if (!success) {
            // 今日已签到
            return null;
        }

        Long userId = Long.valueOf(loginUserId.toString());

        // 1. 写签到记录，并计算连续天数和连续奖励积分
        SignInVO signInVO = userSignInService.recordSignIn(userId, LocalDate.now());
        int bonusPoints = signInVO.getBonusPoints();

        // 2. 基础签到积分（同原逻辑：加入 points，更新 lastSignInDate）
        updatePoints(userId, PointConstant.SIGN_IN_POINT, true);

        // 记录基础签到积分流水
        UserPoints userPoints = this.getById(userId);
        int beforePoints = userPoints.getPoints() - PointConstant.SIGN_IN_POINT;
        int afterPoints = userPoints.getPoints();
        int usedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
        userPointsRecordService.addPointsIncreaseRecord(userId, PointConstant.SIGN_IN_POINT,
                SIGN_IN.getValue(), "每日签到奖励（连续第 " + signInVO.getContinuousDays() + " 天）",
                beforePoints, afterPoints, usedPoints, usedPoints);

        // 3. 连续签到额外奖励：和 VIP 逻辑一样，通过 updateUsedPoints(-bonus) 加到可用积分
        if (bonusPoints > 0) {
            updateUsedPoints(userId, -bonusPoints);
            UserPoints afterBonus = this.getById(userId);
            int bonusBeforeUsed = afterBonus.getUsedPoints() + bonusPoints;
            userPointsRecordService.addPointsIncreaseRecord(userId, bonusPoints,
                    SIGN_IN.getValue(), "连续签到第 " + signInVO.getContinuousDays() + " 天额外奖励",
                    afterBonus.getPoints(), afterBonus.getPoints(),
                    bonusBeforeUsed, afterBonus.getUsedPoints());
        }

        // 4. VIP 签到返还基础积分（原有逻辑不变）
        if (isUserVip(userId)) {
            updateUsedPoints(userId, -PointConstant.SIGN_IN_POINT);
            UserPoints vipUserPoints = this.getById(userId);
            int vipBeforeUsedPoints = vipUserPoints.getUsedPoints() + PointConstant.SIGN_IN_POINT;
            userPointsRecordService.addPointsIncreaseRecord(userId, PointConstant.SIGN_IN_POINT,
                    SIGN_IN.getValue(), "VIP签到积分返还",
                    vipUserPoints.getPoints(), vipUserPoints.getPoints(),
                    vipBeforeUsedPoints, vipUserPoints.getUsedPoints());
        }

        return signInVO;
    }

    @Override
    public void updatePoints(Long userId, Integer points, boolean isSignIn) {
        runWithUserPointsLock(userId, () -> {
            UserPoints userPoints = this.getById(userId);
            userPoints.setPoints(userPoints.getPoints() + points);
            userPoints.setLevel(calculateLevel(userPoints.getPoints()));
            if (isSignIn) {
                userPoints.setLastSignInDate(new Date());
            }
            this.updateById(userPoints);
        });
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
        runWithUserPointsLock(userId, () -> {
            UserPoints userPoints = this.getById(userId);
            int used = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
            userPoints.setUsedPoints(used + points);
            this.updateById(userPoints);
        });
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
        deductPoints(userId, pointsToDeduct, OTHER.getValue(), null, "积分扣除");
    }

    @Override
    public void deductPoints(Long userId, Integer pointsToDeduct, String sourceType, String sourceId, String description) {
        runWithUserPointsLock(userId, () -> doDeductPoints(userId, pointsToDeduct, sourceType, sourceId, description));
    }

    private void doDeductPoints(Long userId, Integer pointsToDeduct, String sourceType, String sourceId, String description) {
        UserPoints userPoints = this.getById(userId);
        ThrowUtils.throwIf(userPoints == null, ErrorCode.NOT_FOUND_ERROR, "用户积分不存在");
        int total = userPoints.getPoints() == null ? 0 : userPoints.getPoints();
        int used = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
        ThrowUtils.throwIf(total - used < pointsToDeduct, ErrorCode.OPERATION_ERROR, "用户积分不足");
        int beforeUsedPoints = used;
        userPoints.setUsedPoints(used + pointsToDeduct);
        this.updateById(userPoints);

        userPointsRecordService.addPointsRecord(userId, 2, pointsToDeduct,
                userPoints.getPoints(), userPoints.getPoints(),
                beforeUsedPoints, userPoints.getUsedPoints(),
                sourceType, sourceId, description);
        notifyAdminIfLargePointsConsume(userId, pointsToDeduct, description);
    }

    @Override
    public void updateUsedPoints(Long userId, Integer points, String sourceType, String sourceId, String description) {
        runWithUserPointsLock(userId, () -> doUpdateUsedPoints(userId, points, sourceType, sourceId, description));
    }

    private void doUpdateUsedPoints(Long userId, Integer points, String sourceType, String sourceId, String description) {
        UserPoints userPoints = this.getById(userId);
        int beforeUsedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
        int afterUsedPoints = beforeUsedPoints + points;
        userPoints.setUsedPoints(afterUsedPoints);
        this.updateById(userPoints);

        if (points < 0) {
            userPointsRecordService.addPointsRecord(userId, 1, -points,
                    userPoints.getPoints(), userPoints.getPoints(),
                    beforeUsedPoints, afterUsedPoints,
                    sourceType, sourceId, description);
        } else {
            userPointsRecordService.addPointsRecord(userId, 2, points,
                    userPoints.getPoints(), userPoints.getPoints(),
                    beforeUsedPoints, afterUsedPoints,
                    sourceType, sourceId, description);
            notifyAdminIfLargePointsConsume(userId, points, description);
        }
    }

    /**
     * 单次消耗/使用积分超过阈值时，通知管理员（直接写库，避免 Service 循环依赖）
     */
    private void notifyAdminIfLargePointsConsume(Long userId, int amount, String description) {
        if (amount <= PointConstant.LARGE_POINTS_CONSUME_THRESHOLD) {
            return;
        }
        String desc = (description != null && !description.isEmpty()) ? description : "积分变动";
        String message = String.format("用户 %d 单次消耗/使用积分 %d：%s", userId, amount, desc);

        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.SYSTEM);
        event.setSourceType(SourceTypeConstant.SYSTEM);
        event.setSourceContent(message);
        event.setRecipientId(POINTS_ALERT_ADMIN_USER_ID);
        event.setRemindTime(new Date());
        event.setUrl("");
        event.setSourceId(-1L);
        event.setSenderId(-1L);
        event.setState(0);
        eventRemindMapper.insert(event);
    }

    @Override
    public void runWithUserPointsLocks(Long[] userIds, Runnable action) {
        Long[] sorted = Arrays.stream(userIds)
                .filter(id -> id != null)
                .distinct()
                .sorted()
                .toArray(Long[]::new);
        if (sorted.length == 0) {
            action.run();
            return;
        }
        RLock[] locks = Arrays.stream(sorted)
                .map(id -> redissonClient.getLock(USER_POINTS_LOCK_PREFIX + id))
                .toArray(RLock[]::new);
        try {
            for (RLock lock : locks) {
                boolean acquired = lock.tryLock(POINTS_LOCK_WAIT_SECONDS, POINTS_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作频繁，请稍后再试");
                }
            }
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后再试");
        } finally {
            for (int i = locks.length - 1; i >= 0; i--) {
                if (locks[i].isHeldByCurrentThread()) {
                    locks[i].unlock();
                }
            }
        }
    }

    private void runWithUserPointsLock(Long userId, Runnable action) {
        runWithUserPointsLocks(new Long[]{userId}, action);
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




