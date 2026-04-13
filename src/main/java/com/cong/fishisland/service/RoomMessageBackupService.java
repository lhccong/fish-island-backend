package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.chat.RoomMessageBackup;

/**
 * 聊天记录备份 Service
 *
 * @author cong
 */
public interface RoomMessageBackupService extends IService<RoomMessageBackup> {

    /**
     * 将一周前的聊天记录迁移到备份表，并从原表删除
     */
    void backupOldMessages();
}
