package com.cong.fishisland.service.impl;

import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.EventRemindService;
import com.cong.fishisland.service.ScriptBehaviorDetectService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 脚本行为检测服务实现
 *
 * @author cong
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScriptBehaviorDetectServiceImpl implements ScriptBehaviorDetectService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final EventRemindService eventRemindService;

    /** 脚本用户标记 key 前缀 */
    public static final String SCRIPT_USER_KEY_PREFIX = "redpacket:grab:script:";
    /** 脚本标记 TTL（24小时） */
    private static final long SCRIPT_MARK_TTL_SECONDS = 24 * 60 * 60;
    /** 固定间隔检测：保留最近多少次时间戳 */
    private static final int TS_HISTORY_SIZE = 6;
    /** 固定间隔检测：间隔标准差低于此值（毫秒）视为固定间隔 */
    private static final double INTERVAL_STD_THRESHOLD_MS = 500.0;

    @Override
    public boolean isScriptUser(Long userId) {
        String scriptKey = SCRIPT_USER_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(scriptKey));
    }

    @Override
    public void checkFixedIntervalBehavior(Long userId, String tsKeyPrefix, String actionLabel) {
        String tsKey = tsKeyPrefix + userId;
        long now = System.currentTimeMillis();

        redisTemplate.opsForList().rightPush(tsKey, String.valueOf(now));
        redisTemplate.opsForList().trim(tsKey, -TS_HISTORY_SIZE, -1);
        redisTemplate.expire(tsKey, Duration.ofDays(1));

        Long size = redisTemplate.opsForList().size(tsKey);
        if (size == null || size < TS_HISTORY_SIZE) {
            return;
        }

        List<Object> rawList = redisTemplate.opsForList().range(tsKey, 0, -1);
        if (rawList == null || rawList.size() < TS_HISTORY_SIZE) {
            return;
        }

        long[] intervals = new long[rawList.size() - 1];
        for (int i = 1; i < rawList.size(); i++) {
            long t1 = Long.parseLong(rawList.get(i - 1).toString());
            long t2 = Long.parseLong(rawList.get(i).toString());
            intervals[i - 1] = t2 - t1;
        }

        double mean = Arrays.stream(intervals).average().orElse(0);
        double variance = Arrays.stream(intervals)
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average().orElse(0);
        double std = Math.sqrt(variance);

        log.info("用户 {} {}间隔检测：均值={}ms，标准差={}ms", userId, actionLabel, (long) mean, (long) std);

        if (std < INTERVAL_STD_THRESHOLD_MS) {
            markAsScriptUser(userId,
                    String.format("检测到用户 %s %s间隔高度一致（标准差 %dms），已标记为脚本用户",
                            getUserDisplayName(userId), actionLabel, (long) std));
        }
    }

    @Override
    public void markAsScriptUser(Long userId, String reason) {
        String scriptKey = SCRIPT_USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(scriptKey, "1", SCRIPT_MARK_TTL_SECONDS, TimeUnit.SECONDS);
        log.warn("用户 {} 被标记为脚本用户：{}", userId, reason);
        eventRemindService.sendSystemNotify(1L, reason);
    }

    @Override
    public void markScriptUser(Long userId, boolean mark) {
        String scriptKey = SCRIPT_USER_KEY_PREFIX + userId;
        if (mark) {
            redisTemplate.opsForValue().set(scriptKey, "1", SCRIPT_MARK_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("管理员手动标记用户 {} 为脚本用户", userId);
        } else {
            redisTemplate.delete(scriptKey);
            log.info("管理员手动取消用户 {} 的脚本标记", userId);
        }
    }

    private String getUserDisplayName(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return String.valueOf(userId);
        }
        return user.getUserName() + ":" + user.getId();
    }
}
