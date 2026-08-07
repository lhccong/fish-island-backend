package com.cong.fishisland.game.manager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.cong.fishisland.game.model.player.GamePlayer;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏会话管理器
 *
 * @author cong
 */
@Slf4j
@Component
public class GameSessionManager {

    private final WebSocketService webSocketService;

    public GameSessionManager(@Lazy WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    /**
     * UserId -> GamePlayer (当前游戏中)
     */
    private final Map<Long, GamePlayer> playingUsers = new ConcurrentHashMap<>();

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, String message) {
        WSBaseResp<?> resp = JSON.parseObject(message, WSBaseResp.class);
        webSocketService.sendToUid(resp, userId);
        log.debug("发送消息给用户: userId={}", userId);
    }

    /**
     * 发送消息给多个用户
     */
    public void sendToUsers(Iterable<Long> userIds, String message) {
        for (Long userId : userIds) {
            sendToUser(userId, message);
        }
    }

    /**
     * 广播消息给房间内所有用户
     */
    public void broadcastToRoom(Iterable<Long> userIds, String message) {
        sendToUsers(userIds, message);
    }

    /**
     * 广播消息给房间内除指定用户外的所有用户
     */
    public void broadcastToRoomExcept(Long excludeUserId, Iterable<Long> userIds, String message) {
        for (Long userId : userIds) {
            if (!userId.equals(excludeUserId)) {
                sendToUser(userId, message);
            }
        }
    }

    /**
     * 广播消息给房间内所有用户
     */
    public void broadcastToRoom(Iterable<Long> userIds, String type, Object data) {
        String message = buildMessage(type, data);
        broadcastToRoom(userIds, message);
    }

    /**
     * 广播消息给房间内除指定用户外的所有用户
     */
    public void broadcastToRoomExcept(Long excludeUserId, Iterable<Long> userIds, String type, Object data) {
        String message = buildMessage(type, data);
        broadcastToRoomExcept(excludeUserId, userIds, message);
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcastToAll(String type, Object data) {
        String message = buildMessage(type, data);
        webSocketService.sendToAllOnline(JSON.parseObject(message, WSBaseResp.class));
        log.debug("广播消息给所有在线用户: type={}", type);
    }

    private String buildMessage(String type, Object data) {
        return JSON.toJSONString(WSBaseResp.builder().type(type).data(data).build(), JSONWriter.Feature.WriteLongAsString);
    }

    /**
     * 设置用户正在游戏中
     */
    public void setPlaying(Long userId, GamePlayer player) {
        playingUsers.put(userId, player);
    }

    /**
     * 获取正在游戏的玩家
     */
    public GamePlayer getPlayingPlayer(Long userId) {
        return playingUsers.get(userId);
    }

    /**
     * 移除游戏中的用户
     */
    public void removePlaying(Long userId) {
        playingUsers.remove(userId);
    }

    /**
     * 检查用户是否正在游戏中
     */
    public boolean isPlaying(Long userId) {
        return playingUsers.containsKey(userId);
    }

    /**
     * 获取正在游戏的人数
     */
    public int getPlayingCount() {
        return playingUsers.size();
    }
}
