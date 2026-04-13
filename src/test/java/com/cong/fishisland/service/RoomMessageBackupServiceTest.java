package com.cong.fishisland.service;

import com.cong.fishisland.common.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 聊天记录备份 Service 真实集成测试
 */
@SpringBootTest
public class RoomMessageBackupServiceTest extends TestBase {

    @Resource
    private RoomMessageBackupService roomMessageBackupService;

    @Test
    void testBackupOldMessages_movesOldToBackupAndDeletesFromOrigin() {
        roomMessageBackupService.backupOldMessages();
    }

}
