package com.cong.fishisland.service;

import com.cong.fishisland.common.TestBase;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.KoishiConfig;
import com.cong.fishisland.service.impl.chat.KoishiWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Koishi WebSocket 服务测试
 */
@Slf4j
class KoishiWebSocketServiceTest extends TestBase {

    @Resource
    private KoishiWebSocketService koishiWebSocketService;

    /**
     * 集成测试：复用启动时已建立的 WebSocket 连接获取回复。
     */
    @Test
    void testGetKoishiReplyIntegration() {
        String reply = koishiWebSocketService.getKoishiReply("Grace", "疯狂星期四");
        log.info("Koishi 回复: {}", reply);
        assertTrue(StringUtils.hasText(reply), "Koishi 回复不应为空");
    }
}
