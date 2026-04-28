package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.BattleRoom;
import com.cong.fishisland.model.fishbattle.battle.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 战斗广播服务。
 * 基于 netty-socketio 的 sendEvent API 直接发送 Socket.IO 事件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Battle3dBroadcastService {

    /**
     * 向房间内所有在线玩家广播事件。
     */
    public void broadcast(BattleRoom room, String eventType, Object data) {
        room.getPlayers().forEach(player -> sendToPlayer(player, eventType, data));
    }

    /**
     * 向指定玩家发送事件。
     */
    public void sendToPlayer(PlayerSession playerSession, String eventType, Object data) {
        if (playerSession.getClient() == null || !playerSession.getClient().isChannelOpen()) {
            return;
        }
        try {
            playerSession.getClient().sendEvent(eventType, data);
        } catch (Exception exception) {
            log.warn("战斗消息发送失败，eventType={}，sessionId={}", eventType, playerSession.getSessionId(), exception);
        }
    }

    /**
     * 构建标准战斗事件基础载荷。
     */
    public Map<String, Object> createBaseCombatEvent(BattleRoom room, String eventIdPrefix, long serverTime) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        long sequence = room.getSequence().incrementAndGet();
        payload.put("eventId", eventIdPrefix + "-" + sequence);
        payload.put("sequence", sequence);
        payload.put("roomId", room.getRoomId());
        payload.put("serverTime", serverTime);
        return payload;
    }
}
