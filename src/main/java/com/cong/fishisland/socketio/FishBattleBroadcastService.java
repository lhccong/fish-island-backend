package com.cong.fishisland.socketio;

import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 摸鱼大乱斗广播服务。
 * 基于 netty-socketio 的 sendEvent API 直接发送 Socket.IO 事件。
 */
@Slf4j
@Service
public class FishBattleBroadcastService {

    /**
     * 向单个客户端发送事件
     */
    public void sendToClient(SocketIOClient client, String eventType, Object data) {
        if (client == null || !client.isChannelOpen()) {
            return;
        }
        try {
            client.sendEvent(eventType, data);
        } catch (Exception e) {
            log.warn("摸鱼大乱斗消息发送失败，eventType={}，sessionId={}", eventType, client.getSessionId(), e);
        }
    }

    /**
     * 向多个客户端广播事件
     */
    public void broadcast(Collection<SocketIOClient> clients, String eventType, Object data) {
        if (clients == null || clients.isEmpty()) {
            return;
        }
        clients.forEach(client -> sendToClient(client, eventType, data));
    }
}
