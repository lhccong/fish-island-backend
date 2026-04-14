package com.cong.fishisland.job.cycle;

import com.cong.fishisland.service.IndexPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 指数持仓份额解锁定时任务
 * 每日 09:30 执行，批量解锁所有用户的 lockedShares
 *
 * @author shing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IndexTradeSettleJob {

    private final IndexPositionService indexPositionService;

    /**
     * 每天 08:30 执行份额解锁
     */
    @Scheduled(cron = "0 30 8 * * ?")
    public void unlockShares() {
        log.info("开始执行份额解锁任务");
        try {
            indexPositionService.unlockAllLockedShares();
            log.info("份额解锁完成");
        } catch (Exception e) {
            log.error("份额解锁任务异常", e);
        }
    }
}
