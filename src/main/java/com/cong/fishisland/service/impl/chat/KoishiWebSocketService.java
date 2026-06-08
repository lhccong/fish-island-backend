package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.KoishiConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Koishi WebSocket 客户端，启动时建立长连接并复用，仅提取机器人回复内容。
 */
@Service
@Slf4j
public class KoishiWebSocketService {

    private static final String TYPE_SANDBOX_MESSAGE = "sandbox/message";
    private static final String KOISHI_USER = "Koishi";

    private final KoishiConfig koishiConfig;
    private final ReentrantLock requestLock = new ReentrantLock();

    private OkHttpClient client;
    private volatile WebSocket webSocket;
    private volatile boolean ready;
    private volatile boolean shuttingDown;
    private volatile CompletableFuture<String> currentPending;
    private volatile CompletableFuture<Void> connectionReady = new CompletableFuture<>();

    public KoishiWebSocketService(KoishiConfig koishiConfig) {
        this.koishiConfig = koishiConfig;
    }

    @PostConstruct
    public void start() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        connect();
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        ready = false;
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.close(1000, "shutdown");
        }
        completePendingExceptionally("Koishi WebSocket 服务已关闭");
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }

    /**
     * 使用默认用户发送消息，仅返回 Koishi 回复内容。
     */
    public String getKoishiReply(String content) {
        return getKoishiReply(koishiConfig.getDefaultUser(), content);
    }

    /**
     * 通过已建立的 WebSocket 连接发送消息，仅返回 Koishi 机器人的回复内容。
     */
    public String getKoishiReply(String username, String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        String user = StringUtils.hasText(username) ? username.trim() : koishiConfig.getDefaultUser();
        waitForConnectionReady();

        requestLock.lock();
        try {
            WebSocket ws = webSocket;
            if (!ready || ws == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Koishi WebSocket 未就绪");
            }

            CompletableFuture<String> pending = new CompletableFuture<>();
            currentPending = pending;

            long now = System.currentTimeMillis();
            send(ws, "create_user_" + now, "sandbox/set-user",
                    koishiConfig.getPlatform(), user, new JSONObject());
            send(ws, "msg_" + now, "sandbox/send-message",
                    koishiConfig.getPlatform(), user, koishiConfig.getChannel(), content, null);

            try {
                return pending.get(koishiConfig.getReplyTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("等待 Koishi 回复超时（{} 秒），返回空字符串", koishiConfig.getReplyTimeoutSeconds());
                return "";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "等待 Koishi 回复被中断");
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "等待 Koishi 回复失败: " + e.getMessage());
            } finally {
                if (currentPending == pending) {
                    currentPending = null;
                }
            }
        } finally {
            requestLock.unlock();
        }
    }

    /**
     * 从 WebSocket 消息中提取 Koishi 回复，忽略用户消息和发送成功响应。
     */
    String extractKoishiReply(String text, String currentUser) {
        JSONObject data;
        try {
            data = JSON.parseObject(text);
        } catch (Exception e) {
            log.warn("Koishi WS 消息不是合法 JSON: {}", text);
            return null;
        }
        if (data == null || !TYPE_SANDBOX_MESSAGE.equals(data.getString("type"))) {
            return null;
        }
        JSONObject body = data.getJSONObject("body");
        if (body == null) {
            return null;
        }
        String msgUser = body.getString("user");
        if (currentUser.equals(msgUser)) {
            return null;
        }
        if (KOISHI_USER.equals(msgUser)) {
            return body.getString("content");
        }
        return null;
    }

    private void connect() {
        if (shuttingDown) {
            return;
        }
        log.info("正在连接 Koishi WebSocket: {}", koishiConfig.getWsUrl());
        Request request = new Request.Builder().url(koishiConfig.getWsUrl()).build();
        client.newWebSocket(request, createListener());
    }

    private WebSocketListener createListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                KoishiWebSocketService.this.webSocket = webSocket;
                log.info("Koishi WebSocket 已连接");
                send(webSocket, "login_" + System.currentTimeMillis(), "login/token", 0, koishiConfig.getToken());

                client.dispatcher().executorService().execute(() -> {
                    try {
                        Thread.sleep(500);
                        if (shuttingDown) {
                            return;
                        }
                        send(webSocket, "create_user_" + System.currentTimeMillis(), "sandbox/set-user",
                                koishiConfig.getPlatform(), koishiConfig.getDefaultUser(), new JSONObject());
                        ready = true;
                        connectionReady.complete(null);
                        log.info("Koishi WebSocket 已就绪");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        onDisconnected("初始化被中断");
                    }
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                log.debug("Koishi WS 收到: {}", text);
                dispatchKoishiReply(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("Koishi WebSocket 连接失败", t);
                onDisconnected(t.getMessage());
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.warn("Koishi WebSocket 已关闭: {} {}", code, reason);
                onDisconnected(reason);
            }
        };
    }

    private void dispatchKoishiReply(String text) {
        String content = extractKoishiContent(text);
        if (content == null) {
            return;
        }
        CompletableFuture<String> pending = currentPending;
        if (pending != null && !pending.isDone()) {
            pending.complete(content);
        }
    }

    private String extractKoishiContent(String text) {
        JSONObject data;
        try {
            data = JSON.parseObject(text);
        } catch (Exception e) {
            return null;
        }
        if (data == null || !TYPE_SANDBOX_MESSAGE.equals(data.getString("type"))) {
            return null;
        }
        JSONObject body = data.getJSONObject("body");
        if (body == null || !KOISHI_USER.equals(body.getString("user"))) {
            return null;
        }
        return body.getString("content");
    }

    private void waitForConnectionReady() {
        if (ready) {
            return;
        }
        try {
            connectionReady.get(koishiConfig.getConnectTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Koishi WebSocket 连接超时");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "Koishi WebSocket 未就绪: " + e.getMessage());
        }
    }

    private void onDisconnected(String reason) {
        ready = false;
        webSocket = null;
        CompletableFuture<Void> previousReady = connectionReady;
        if (!previousReady.isDone()) {
            previousReady.completeExceptionally(
                    new BusinessException(ErrorCode.OPERATION_ERROR, "Koishi WebSocket 连接失败: " + reason));
        }
        connectionReady = new CompletableFuture<>();
        completePendingExceptionally("Koishi WebSocket 连接断开: " + reason);
        if (!shuttingDown) {
            scheduleReconnect();
        }
    }

    private void completePendingExceptionally(String message) {
        CompletableFuture<String> pending = currentPending;
        if (pending != null && !pending.isDone()) {
            pending.completeExceptionally(new BusinessException(ErrorCode.OPERATION_ERROR, message));
        }
    }

    private void scheduleReconnect() {
        client.dispatcher().executorService().execute(() -> {
            try {
                Thread.sleep(koishiConfig.getReconnectDelaySeconds() * 1000L);
                if (!shuttingDown) {
                    connect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void send(WebSocket webSocket, String id, String type, Object... args) {
        JSONObject msg = new JSONObject();
        msg.put("id", id);
        msg.put("type", type);
        msg.put("args", args);
        String payload = msg.toJSONString();
        log.debug("Koishi WS 发送: {}", payload);
        webSocket.send(payload);
    }
}
