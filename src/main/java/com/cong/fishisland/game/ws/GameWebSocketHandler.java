package com.cong.fishisland.game.ws;

import com.alibaba.fastjson2.JSON;
import com.cong.fishisland.game.model.dto.response.GameMessageResult;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏 WebSocket 处理器
 * 负责处理游戏消息的发送逻辑
 *
 * @author cong
 */
@Slf4j
@Component
public class GameWebSocketHandler {

    /**
     * 发送游戏消息
     *
     * @param channel    WebSocket 通道
     * @param gameResult 游戏消息结果
     */
    public void sendGameMessage(Channel channel, GameMessageResult gameResult) {
        if (gameResult == null) {
            return;
        }

        if (gameResult.getCode() == 0) {
            // 成功：发送已序列化的 JSON（Long 已转为字符串）
            channel.writeAndFlush(new TextWebSocketFrame(gameResult.toJson()));
        } else {
            // 失败：发送错误信息
            String json = JSON.toJSONString(WSBaseResp.builder()
                    .type("error")
                    .data(gameResult.getMessage())
                    .build());
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    /**
     * 处理游戏消息
     *
     * @param handler     游戏消息处理器
     * @param messageType 消息类型
     * @param content     消息内容
     * @param userId      用户ID
     * @param channel     WebSocket 通道
     */
    public void handleGameMessage(GameMessageHandler handler, String messageType,
                                   String content, Long userId, Channel channel) {
        Object result = handler.handle(messageType, content, userId);
        if (result instanceof GameMessageResult) {
            sendGameMessage(channel, (GameMessageResult) result);
        }
    }
}
