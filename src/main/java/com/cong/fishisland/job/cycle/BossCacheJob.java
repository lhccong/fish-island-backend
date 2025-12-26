package com.cong.fishisland.job.cycle;

import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.model.vo.game.BossVO;
import com.cong.fishisland.service.BossService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Boss数据缓存定时任务
 * 每天凌晨12点将Boss数据写入Redis缓存
 *
 * @author cong
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BossCacheJob {

    private final BossService bossService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 每天凌晨12点执行，将Boss数据写入Redis缓存
     * cron表达式：0 0 0 * * ? 表示每天凌晨0点0分0秒执行
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cacheBossList() {
        log.info("开始执行Boss数据缓存任务");

        try {
            // 1. 获取Boss列表数据
            List<BossVO> bossList = bossService.getBossList();

            if (bossList == null || bossList.isEmpty()) {
                log.warn("Boss列表为空，跳过缓存");
                return;
            }

            // 2. 将Boss列表序列化为JSON字符串
            String bossListJson = objectMapper.writeValueAsString(bossList);

            // 3. 将数据写入Redis，设置过期时间为24小时（确保每天都会更新）
            String cacheKey = RedisKey.getKey(RedisKey.BOSS_LIST_CACHE_KEY);
            redisTemplate.opsForValue().set(cacheKey, bossListJson, 24, TimeUnit.HOURS);

            // 4. 初始化每个Boss的血量到Redis（每天重置Boss血量）
            for (BossVO boss : bossList) {
                String healthKey = RedisKey.getKey(RedisKey.BOSS_HEALTH_CACHE_KEY, boss.getId());
                redisTemplate.opsForValue().set(
                        healthKey,
                        String.valueOf(boss.getHealth()),
                        24,
                        TimeUnit.HOURS
                );
            }

            log.info("Boss数据缓存成功，共缓存{}个Boss，缓存key: {}", bossList.size(), cacheKey);
        } catch (Exception e) {
            log.error("Boss数据缓存任务执行异常", e);
        }

        log.info("Boss数据缓存任务执行完成");
    }
}

