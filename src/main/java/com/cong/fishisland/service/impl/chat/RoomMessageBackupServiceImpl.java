package com.cong.fishisland.service.impl.chat;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.config.ThreadPoolConfig;
import com.cong.fishisland.mapper.chat.RoomMessageBackupMapper;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.chat.RoomMessageBackup;
import com.cong.fishisland.service.RoomMessageBackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 聊天记录备份 Service 实现
 *
 * @author cong
 */
@Slf4j
@Service
public class RoomMessageBackupServiceImpl extends ServiceImpl<RoomMessageBackupMapper, RoomMessageBackup>
        implements RoomMessageBackupService {

    private static final int BATCH_SIZE = 500;

    @Resource(name = ThreadPoolConfig.WANWU_EXECUTOR)
    private ThreadPoolTaskExecutor fishExecutor;


    @Resource
    private RoomMessageBackupBatchWriter batchWriter;

    @Override
    public void backupOldMessages() {
        long twoWeeksMillis = 7L * 24 * 60 * 60 * 1000;
        Date twoWeeksAgo = new Date(System.currentTimeMillis() - twoWeeksMillis);

        List<RoomMessage> oldMessages = baseMapper.selectOldMessages(twoWeeksAgo);
        if (oldMessages.isEmpty()) {
            log.info("没有需要备份的聊天记录");
            return;
        }

        log.info("开始备份聊天记录，共 {} 条，分批大小 {}", oldMessages.size(), BATCH_SIZE);

        // 并行流加速数据转换（纯 CPU，无 IO）
        Date backupTime = new Date();
        List<RoomMessageBackup> backupList = oldMessages.parallelStream().map(msg -> {
            RoomMessageBackup backup = new RoomMessageBackup();
            backup.setId(msg.getId());
            backup.setUserId(msg.getUserId());
            backup.setRoomId(msg.getRoomId());
            backup.setMessageJson(msg.getMessageJson());
            backup.setMessageId(msg.getMessageId());
            backup.setCreateTime(msg.getCreateTime());
            backup.setUpdateTime(msg.getUpdateTime());
            backup.setIsDelete(msg.getIsDelete());
            backup.setBackupTime(backupTime);
            return backup;
        }).collect(Collectors.toList());

        // 分批，每批提交给线程池，各自开独立事务写入

        // 等待所有批次完成，任意一批异常则向上抛，不执行删除
        CompletableFuture.allOf(partition(backupList, BATCH_SIZE).stream()
                .map(batch -> CompletableFuture.runAsync(
                        () -> batchWriter.writeBatch(batch), fishExecutor)).toArray(CompletableFuture[]::new)).join();

        // 全部备份成功后物理删除原表
        int deleted = baseMapper.deleteOldMessages(twoWeeksAgo);
        log.info("聊天记录备份完成，共备份 {} 条，物理删除 {} 条", backupList.size(), deleted);
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
