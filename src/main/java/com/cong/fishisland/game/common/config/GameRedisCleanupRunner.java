package com.cong.fishisland.game.common.config;

import com.cong.fishisland.game.common.constant.GameRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 服务启动时清理游戏 Redis 数据
 * 防止服务掉线重启后用户卡在房间导致无法进入其他房间
 *
 * @author cong
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "fish.game.redis.startup-cleanup", havingValue = "true", matchIfMissing = true)
public class GameRedisCleanupRunner implements ApplicationRunner {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String baseKey = GameRedisKey.getKey("");
            Set<String> gameKeys = stringRedisTemplate.keys(baseKey + "*");
            if (gameKeys != null && !gameKeys.isEmpty()) {
                stringRedisTemplate.delete(gameKeys);
            }
        } catch (Exception e) {
            log.error("清理游戏 Redis 数据失败", e);
        }
    }
}
