package com.cong.fishisland.service.impl.emoticon;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cong.fishisland.mapper.emoticon.EmoticonFavourMapper;
import com.cong.fishisland.model.entity.emoticon.EmoticonFavour;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
class EmoticonOcrWorker {
    private static final long LEASE_MILLIS = TimeUnit.MINUTES.toMillis(3);
    private final EmoticonFavourMapper mapper;
    private final EmoticonOcr ocr;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(),
            task -> new Thread(task, "emoticon-ocr"),
            new ThreadPoolExecutor.DiscardPolicy()); // A busy worker will pick up persisted jobs on later ticks.

    EmoticonOcrWorker(EmoticonFavourMapper mapper, EmoticonOcr ocr) {
        this.mapper = mapper;
        this.ocr = ocr;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void poll() {
        executor.execute(() -> {
            try {
                processNext();
            } catch (RuntimeException failure) {
                log.warn("表情包识别任务暂不可用（{}）", failure.getClass().getSimpleName());
            }
        });
    }

    void processNext() {
        Date now = new Date();
        EmoticonFavour favourite = mapper.selectOne(Wrappers.<EmoticonFavour>lambdaQuery()
                .le(EmoticonFavour::getOcrNextAttemptAt, now)
                .orderByAsc(EmoticonFavour::getOcrNextAttemptAt, EmoticonFavour::getId)
                .last("LIMIT 1"));
        if (favourite == null) {
            return;
        }
        Date lease = new Date(now.getTime() + LEASE_MILLIS);
        int claimed = mapper.update(null, Wrappers.<EmoticonFavour>lambdaUpdate()
                .eq(EmoticonFavour::getId, favourite.getId())
                .eq(EmoticonFavour::getOcrNextAttemptAt, favourite.getOcrNextAttemptAt())
                .set(EmoticonFavour::getOcrNextAttemptAt, lease));
        if (claimed == 0) {
            return;
        }

        String text = null;
        Date nextAttempt = null;
        int attempts = favourite.getOcrAttempts();
        try {
            text = ocr.recognize(favourite.getEmoticonSrc());
        } catch (IOException | RuntimeException | LinkageError failure) {
            attempts++;
            if (attempts < 3) {
                nextAttempt = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(attempts));
            } else {
                log.warn("表情包识别失败，已用尽重试次数（{}）", failure.getClass().getSimpleName());
            }
        }
        mapper.update(null, Wrappers.<EmoticonFavour>lambdaUpdate()
                .eq(EmoticonFavour::getId, favourite.getId())
                .eq(EmoticonFavour::getOcrNextAttemptAt, lease)
                .set(EmoticonFavour::getOcrText, text)
                .set(EmoticonFavour::getOcrAttempts, attempts)
                .set(EmoticonFavour::getOcrNextAttemptAt, nextAttempt));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
