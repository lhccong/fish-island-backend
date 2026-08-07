package com.cong.fishisland.game.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.cong.fishisland.game.constant.GameRedisKey;
import com.cong.fishisland.game.model.GameSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 游戏会话 Redis 缓存
 *
 * @author cong
 */
@Slf4j
@Component
public class GameSessionRedisCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    // ==================== 会话 CRUD ====================

    /**
     * 保存会话
     */
    public void saveSession(GameSession session) {
        if (session == null || session.getUserId() == null) {
            return;
        }
        String key = sessionKey(session.getUserId());
        String json = JSON.toJSONString(session);
        stringRedisTemplate.opsForValue().set(key, json, SESSION_TTL);
    }

    /**
     * 获取会话
     */
    public GameSession getSession(Long userId) {
        if (userId == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(sessionKey(userId));
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, new TypeReference<GameSession>() {
            });
        } catch (Exception e) {
            log.error("解析会话数据失败: userId={}", userId, e);
            // 解析失败时尝试删除旧数据，让用户重新创建会话
            deleteSession(userId);
            return null;
        }
    }

    /**
     * 删除会话
     */
    public void deleteSession(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(sessionKey(userId));
    }

    // ==================== 批量查询 ====================

    /**
     * 获取所有会话（用于重启恢复）
     */
    public List<GameSession> getAllSessions() {
        Set<String> keys = stringRedisTemplate.keys(
                GameRedisKey.getKey(GameRedisKey.USER_SESSION_KEY, "*"));
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<GameSession> result = new ArrayList<>();
        for (String key : keys) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                try {
                    result.add(JSON.parseObject(json, new TypeReference<GameSession>() {
                    }));
                } catch (Exception e) {
                    log.warn("解析会话失败: key={}", key, e);
                }
            }
        }
        return result;
    }

    // ==================== 工具方法 ====================

    private String sessionKey(Long userId) {
        return GameRedisKey.getKey(GameRedisKey.USER_SESSION_KEY, userId.toString());
    }
}