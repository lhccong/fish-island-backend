package com.cong.fishisland.config.fishbattle;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 摸鱼大乱斗服务端配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "fish-battle-server")
public class FishBattleServerProperties {

    /**
     * Socket.IO 监听端口
     */
    private Integer wsPort = 8091;

    /**
     * Socket.IO 监听主机地址
     */
    private String wsHost = "0.0.0.0";

    /**
     * Socket.IO 心跳间隔（毫秒）
     */
    private Integer pingIntervalMs = 10000;

    /**
     * Socket.IO 心跳超时（毫秒）
     */
    private Integer pingTimeoutMs = 30000;

    /**
     * CORS 允许来源，逗号分隔，空字符串表示允许所有
     */
    private String allowedOrigins = "";

    /**
     * 选英雄阶段倒计时（秒），便于调试时调整
     */
    private Integer heroPickDuration = 60;

    /**
     * 是否启用小兵生成（调试用），默认 true
     */
    private Boolean spawnMinionsEnabled = true;

    /**
     * 仅为指定队伍出兵（调试用），blue/red，null 表示双方都出
     */
    private String spawnOnlyTeam;
}
