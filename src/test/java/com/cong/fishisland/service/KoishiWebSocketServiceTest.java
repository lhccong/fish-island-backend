package com.cong.fishisland.service;

import com.cong.fishisland.common.TestBase;
import com.cong.fishisland.service.impl.chat.KoishiWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

/**
 * Koishi WebSocket 服务测试
 */
@Slf4j
class KoishiWebSocketServiceTest extends TestBase {

    @Resource
    private KoishiWebSocketService koishiWebSocketService;

    /**
     * 集成测试：向 Koishi 发送消息，回复由全局 WebSocket 监听器处理。
     */
    @Test
    void testSendMessageIntegration() {
        koishiWebSocketService.sendMessage("Grace", "疯狂星期四", null);
        log.info("Koishi 消息已发送，回复将由全局监听器处理");
    }
}
