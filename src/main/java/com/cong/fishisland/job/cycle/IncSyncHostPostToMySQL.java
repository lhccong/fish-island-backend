package com.cong.fishisland.job.cycle;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.manager.DataSourceRegistry;
import com.cong.fishisland.model.entity.hot.HotPost;
import com.cong.fishisland.model.enums.HotDataKeyEnum;
import com.cong.fishisland.service.HotPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 自动同步热榜数据
 * # @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IncSyncHostPostToMySQL {

    private final DataSourceRegistry dataSourceRegistry;
    private final HotPostService hotPostService;
    private final RetryTemplate retryTemplate;

    /**
     * 每半小时执行一次
     */
    @Scheduled(fixedRate = 1_800_000)
    public void run() {
        log.info("开始更新热榜数据...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        HotDataKeyEnum.getValues().forEach(key -> {
            try {
                retryTemplate.execute(context -> {
                    updateHotPost(key);
                    return null;
                });
            } catch (Exception e) {
                log.error("更新热榜数据失败，已达到最大重试次数，放弃更新【{}】", key, e);
            }
        });
        stopWatch.stop();
        log.info("更新热榜数据完成，耗时：{}ms", stopWatch.getTotalTimeMillis());
    }

    private void updateHotPost(String key) {
        LambdaQueryWrapper<HotPost> hotPostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        hotPostLambdaQueryWrapper.eq(HotPost::getType, key);
        HotPost oldHotPost = hotPostService.getOne(hotPostLambdaQueryWrapper);
        if (oldHotPost != null) {
            //如果更新时间间隔未到直接跳过
            //小时制
            BigDecimal updateInterval = oldHotPost.getUpdateInterval();
            //转成时间戳
            long updateIntervalMillis = updateInterval.multiply(new BigDecimal(60 * 60 * 1000)).longValue();
            if (oldHotPost.getUpdateTime().getTime() + updateIntervalMillis > System.currentTimeMillis()) {
                log.info("加载===========>【{}】热榜数据跳过", HotDataKeyEnum.getEnumByValue(key).getText());
                return;
            }
        }
        HotPost hotPost = dataSourceRegistry.getDataSourceByType(key).getHotPost();
        hotPost.setType(key);
        if (oldHotPost != null) {
            hotPost.setId(oldHotPost.getId());
        }
        hotPost.setUpdateTime(new Date());
        hotPostService.saveOrUpdate(hotPost);
        log.info("加载===========>【{}】热榜数据完成", hotPost.getTypeName());
    }
}
