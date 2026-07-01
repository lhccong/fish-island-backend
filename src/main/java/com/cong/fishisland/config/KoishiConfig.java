package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Koishi WebSocket 配置
 */
@Configuration
@ConfigurationProperties(prefix = "fishisland.koishi")
@Data
public class KoishiConfig {

    private String wsUrl = "ws://127.0.0.1:5140/status";

    private String platform = "sandbox:mock";

    private String defaultUser = "Eve";

    private String channel = "#";

    /**
     * 等待 Koishi 回复的超时时间（秒）
     */
    private int replyTimeoutSeconds = 3;

    /**
     * 启动时等待 WebSocket 就绪的超时时间（秒）
     */
    private int connectTimeoutSeconds = 15;

    /**
     * 断线后重连间隔（秒）
     */
    private int reconnectDelaySeconds = 3;
}
