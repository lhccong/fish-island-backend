package com.cong.fishisland.service.impl.chat;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.chat.RoomMessageBackupMapper;
import com.cong.fishisland.model.entity.chat.RoomMessageBackup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 备份批次写入器 —— 独立 Bean，保证子线程调用时能走 Spring AOP 事务代理
 *
 * @author cong
 */
@Slf4j
@Component
public class RoomMessageBackupBatchWriter extends ServiceImpl<RoomMessageBackupMapper, RoomMessageBackup> {

    /**
     * 每批独立开事务写入，REQUIRES_NEW 确保子线程自己持有连接，不依赖调用方事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeBatch(List<RoomMessageBackup> batch) {
        this.saveBatch(batch);
        log.info("备份批次写入完成，本批 {} 条，线程：{}", batch.size(), Thread.currentThread().getName());
    }
}
