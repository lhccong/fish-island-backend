package com.cong.fishisland.job.cycle;

import com.cong.fishisland.service.RoomMessageBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 聊天记录备份定时任务
 * 每周一凌晨 2 点将一周前的聊天记录迁移到备份表
 *
 * @author cong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBackupJob {

    private final RoomMessageBackupService roomMessageBackupService;

    /**
     * 每周一凌晨 2:00 执行备份
     */
    @Scheduled(cron = "0 0 2 * * MON")
    public void backupChatMessages() {
        log.info("开始执行聊天记录备份任务");
        try {
            roomMessageBackupService.backupOldMessages();
            log.info("聊天记录备份任务执行完成");
        } catch (Exception e) {
            log.error("聊天记录备份任务执行异常", e);
        }
    }
}
