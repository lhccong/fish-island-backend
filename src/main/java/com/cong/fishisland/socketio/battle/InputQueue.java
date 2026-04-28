package com.cong.fishisland.socketio.battle;

import com.cong.fishisland.model.fishbattle.battle.PlayerInput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 线程安全的玩家输入队列（按房间分桶）。
 * <p>
 * Socket.IO 事件线程通过 {@link #enqueue(PlayerInput)} 入队，
 * tick 线程通过 {@link #drainByRoom(String)} 一次性取出指定房间的待处理输入。
 */
@Component
public class InputQueue {

    /** roomId → 该房间的输入队列 */
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<PlayerInput>> roomQueues = new ConcurrentHashMap<>();

    /**
     * 将玩家输入入队。由 Socket.IO 事件线程调用。
     */
    public void enqueue(PlayerInput input) {
        if (input == null || input.getRoomId() == null) {
            return;
        }
        roomQueues.computeIfAbsent(input.getRoomId(), k -> new ConcurrentLinkedQueue<>()).offer(input);
    }

    /**
     * 按房间 ID 排空队列，返回指定房间的待处理输入（按入队顺序）。
     */
    public List<PlayerInput> drainByRoom(String roomId) {
        List<PlayerInput> drained = new ArrayList<>();
        if (roomId == null) {
            return drained;
        }
        ConcurrentLinkedQueue<PlayerInput> queue = roomQueues.get(roomId);
        if (queue == null) {
            return drained;
        }
        PlayerInput input;
        while ((input = queue.poll()) != null) {
            drained.add(input);
        }
        return drained;
    }

    /**
     * 清除指定房间的输入队列。在房间销毁时调用，防止内存泄漏。
     */
    public void cleanupRoom(String roomId) {
        if (roomId != null) {
            roomQueues.remove(roomId);
        }
    }
}
