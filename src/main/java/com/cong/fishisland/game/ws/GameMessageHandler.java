package com.cong.fishisland.game.ws;

import com.cong.fishisland.game.enums.GameTypeEnum;

/**
 * 游戏消息处理器接口
 *
 * @author cong
 */
public interface GameMessageHandler {

    /**
     * 获取该处理器支持的游戏类型
     */
    GameTypeEnum getGameType();

    /**
     * 处理消息
     *
     * @param messageType 消息类型
     * @param jsonContent 消息内容（JSON字符串）
     * @param userId     用户ID
     * @return 处理结果，如果返回null则不发送响应
     *         返回 String 表示已序列化的 JSON 字符串
     *         返回其他对象会自动序列化为 JSON（Long 会被转成字符串）
     */
    Object handle(String messageType, String jsonContent, Long userId);

    /**
     * 用户断开连接时清理资源
     */
    default void onDisconnect(Long userId) {
    }
}