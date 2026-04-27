package com.cong.fishisland.config.fishbattle;

import cn.dev33.satoken.stp.StpUtil;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import javax.annotation.PreDestroy;

/**
 * 摸鱼大乱斗 netty-socketio 服务器配置。
 * 创建并管理 SocketIOServer 实例，支持 Socket.IO 协议与前端 socket.io-client 对接。
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
public class FishBattleSocketIOConfig {

    private final FishBattleServerProperties fishBattleServerProperties;
    private SocketIOServer server;

    /**
     * 创建 SocketIOServer Bean。
     */
    @Bean
    public SocketIOServer fishBattleSocketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(fishBattleServerProperties.getWsHost());
        config.setPort(fishBattleServerProperties.getWsPort());

        config.setPingInterval(fishBattleServerProperties.getPingIntervalMs());
        config.setPingTimeout(fishBattleServerProperties.getPingTimeoutMs());

        // 仅使用 WebSocket 传输，跳过 HTTP 长轮询以降低延迟
        config.setTransports(Transport.WEBSOCKET);

        // CORS 配置
        String allowedOrigins = fishBattleServerProperties.getAllowedOrigins();
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            config.setOrigin(allowedOrigins);
        }

        // TCP 底层优化：禁用 Nagle 算法
        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setTcpNoDelay(true);
        socketConfig.setReuseAddress(true);
        socketConfig.setSoLinger(0);
        config.setSocketConfig(socketConfig);

        // Worker 线程数
        config.setWorkerThreads(Runtime.getRuntime().availableProcessors() * 2);

        // JSON 序列化：Long → String，防止前端 JS 精度丢失（与 REST API JsonConfig 保持一致）
        SimpleModule longToStringModule = new SimpleModule();
        longToStringModule.addSerializer(Long.class, ToStringSerializer.instance);
        longToStringModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        config.setJsonSupport(new JacksonJsonSupport(longToStringModule));

        // 授权：通过握手参数中的 tokenValue 验证 Sa-Token 登录态
        config.setAuthorizationListener(data -> {
            String tokenValue = data.getSingleUrlParam("tokenValue");
            if (tokenValue == null || tokenValue.isEmpty()) {
                log.warn("Socket.IO 连接被拒绝：未携带 token");
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
            try {
                Object loginId = StpUtil.getLoginIdByToken(tokenValue);
                if (loginId == null) {
                    log.warn("Socket.IO 连接被拒绝：token 无效");
                    return AuthorizationResult.FAILED_AUTHORIZATION;
                }
                log.debug("Socket.IO 连接鉴权通过：userId={}", loginId);
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
            } catch (Exception e) {
                log.warn("Socket.IO 连接鉴权异常：{}", e.getMessage());
                return AuthorizationResult.FAILED_AUTHORIZATION;
            }
        });

        server = new SocketIOServer(config);
        return server;
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.stop();
            log.info("摸鱼大乱斗 Socket.IO 服务器已关闭");
        }
    }
}
