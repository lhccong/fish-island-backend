package com.cong.fishisland.model.fishbattle.battle;

import com.corundumstudio.socketio.SocketIOClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 战斗房间内的玩家会话信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSession {
    /**
     * 当前连接对应的 Socket.IO 会话标识。
     */
    private String sessionId;

    /**
     * 当前连接对应的认证用户 ID。
     */
    private Long userId;

    /**
     * 玩家显示名称。
     */
    private String playerName;

    /**
     * 当前分配的英雄实例 ID。
     */
    private String championId;

    /**
     * 当前是否为观战者。
     */
    private Boolean spectator;

    /**
     * netty-socketio 客户端连接对象（替代原生 Netty Channel）。
     */
    private SocketIOClient client;
}
