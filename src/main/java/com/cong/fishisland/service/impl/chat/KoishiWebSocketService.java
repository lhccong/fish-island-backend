package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.KoishiConfig;
import com.cong.fishisland.model.enums.HotDataKeyEnum;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.service.datasource.DataSourceCookieService;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.websocket.service.WebSocketService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Koishi WebSocket 客户端，启动时建立长连接并复用。
 * 发送消息与接收回复分离：发送仅负责投递，回复由全局 onMessage 统一处理。
 */
@Service
@Slf4j
public class KoishiWebSocketService {

    private static final String TYPE_SANDBOX_MESSAGE = "sandbox/message";
    private static final String KOISHI_USER = "Koishi";
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*/?>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AUDIO_TAG_PATTERN = Pattern.compile(
            "<audio\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*/?>",
            Pattern.CASE_INSENSITIVE);

    private final KoishiConfig koishiConfig;
    private final DataSourceCookieService dataSourceCookieService;
    private final RobotChatMessageService robotChatMessageService;
    private final WebSocketService webSocketService;
    private final ReentrantLock requestLock = new ReentrantLock();

    private OkHttpClient client;
    private volatile WebSocket webSocket;
    private volatile boolean ready;
    private volatile boolean shuttingDown;
    private volatile Message currentQuotedMessage;
    private volatile CompletableFuture<Void> connectionReady = new CompletableFuture<>();

    public KoishiWebSocketService(KoishiConfig koishiConfig,
                                  DataSourceCookieService dataSourceCookieService,
                                  RobotChatMessageService robotChatMessageService,
                                  WebSocketService webSocketService) {
        this.koishiConfig = koishiConfig;
        this.dataSourceCookieService = dataSourceCookieService;
        this.robotChatMessageService = robotChatMessageService;
        this.webSocketService = webSocketService;
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
        currentQuotedMessage = null;
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }

    /**
     * 使用默认用户向 Koishi 发送消息。
     */
    public void sendMessage(String content, Message quotedMessage) {
        sendMessage(koishiConfig.getDefaultUser(), content, quotedMessage);
    }

    /**
     * 通过已建立的 WebSocket 连接向 Koishi 发送消息，回复由全局监听器统一处理。
     */
    public void sendMessage(String username, String content, Message quotedMessage) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }
        String user = StringUtils.hasText(username) ? username.trim() : koishiConfig.getDefaultUser();
        try {
            waitForConnectionReady();
        } catch (Exception e) {
            log.warn("Koishi WebSocket 未就绪，消息未发送: {}", e.getMessage());
            return;
        }

        requestLock.lock();
        try {
            WebSocket ws = webSocket;
            if (!ready || ws == null) {
                log.warn("Koishi WebSocket 未就绪，消息未发送");
                return;
            }

            currentQuotedMessage = quotedMessage;

            long now = System.currentTimeMillis();
            send(ws, "create_user_" + now, "sandbox/set-user",
                    koishiConfig.getPlatform(), user, new JSONObject());
            send(ws, "msg_" + now, "sandbox/send-message",
                    koishiConfig.getPlatform(), user, koishiConfig.getChannel(), content, null);
        } finally {
            requestLock.unlock();
        }
    }

    /**
     * 将 Koishi 回复中的 HTML 媒体标签转换为聊天室格式。
     */
    String formatKoishiReply(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        content = replaceMediaTag(content, IMG_TAG_PATTERN, "img", true);
        content = replaceMediaTag(content, AUDIO_TAG_PATTERN, "audio", false);
        return content;
    }

    private String replaceMediaTag(String content, Pattern pattern, String tagName, boolean stripQueryParams) {
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            if (stripQueryParams) {
                int queryIndex = url.indexOf('?');
                if (queryIndex >= 0) {
                    url = url.substring(0, queryIndex);
                }
            }
            String replacement = "[" + tagName + "]" + url + "[/" + tagName + "]";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String resolveToken() {
        return dataSourceCookieService.getEnabledCookie(HotDataKeyEnum.KOISHI.getValue());
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
                send(webSocket, "login_" + System.currentTimeMillis(), "login/token", 0, resolveToken());

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
        String reply = formatKoishiReply(content);
        if (!StringUtils.hasText(reply)) {
            return;
        }

        Message quotedMessage;
        requestLock.lock();
        try {
            quotedMessage = currentQuotedMessage;
            currentQuotedMessage = null;
        } finally {
            requestLock.unlock();
        }

        MessageWrapper messageWrapper = robotChatMessageService.buildAiReplyWrapper(reply, quotedMessage);
        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(messageWrapper).build());
        robotChatMessageService.saveAiReply(messageWrapper);
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
        currentQuotedMessage = null;
        CompletableFuture<Void> previousReady = connectionReady;
        if (!previousReady.isDone()) {
            previousReady.completeExceptionally(
                    new BusinessException(ErrorCode.OPERATION_ERROR, "Koishi WebSocket 连接失败: " + reason));
        }
        connectionReady = new CompletableFuture<>();
        if (!shuttingDown) {
            scheduleReconnect();
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
