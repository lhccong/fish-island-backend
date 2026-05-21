package com.cong.fishisland.job.cycle;

import com.cong.fishisland.service.LuckyBagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 福袋到期开奖任务
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LuckyBagExpireJob {

    private final LuckyBagService luckyBagService;

    /**
     * 每 5 秒扫描一次到期福袋并开奖
     */
    @Scheduled(fixedRate = 5000, initialDelay = 5000)
    public void processExpiredLuckyBags() {
        luckyBagService.processExpiredLuckyBags();
    }
}
