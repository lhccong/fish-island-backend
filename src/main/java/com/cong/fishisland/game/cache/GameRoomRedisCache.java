package com.cong.fishisland.game.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.cong.fishisland.game.constant.GameRedisKey;
import com.cong.fishisland.game.enums.GameTypeEnum;
import com.cong.fishisland.game.model.room.GameRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 游戏房间 Redis 缓存
 *
 * @author cong
 */
@Slf4j
@Component
public class GameRoomRedisCache {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Duration ROOM_TTL = Duration.ofHours(24);

    // ==================== 房间 CRUD ====================

    /**
     * 保存房间
     */
    public void saveRoom(GameRoom room) {
        if (room == null || room.getRoomId() == null) {
            return;
        }
        String roomKey = roomKey(room.getRoomId());
        // 禁用循环引用，避免 $ref 导致解析失败
        String json = JSON.toJSONString(room);
        stringRedisTemplate.opsForValue().set(roomKey, json, ROOM_TTL);

        // 更新索引
        stringRedisTemplate.opsForSet().add(GameRedisKey.getKey(GameRedisKey.ROOMS_ALL), room.getRoomId());
        if (room.getGameType() != null) {
            stringRedisTemplate.opsForSet().add(
                    GameRedisKey.getKey(GameRedisKey.ROOMS_BY_TYPE, room.getGameType().name()),
                    room.getRoomId());
        }
    }

    /**
     * 获取房间
     * 自动清理无法解析的损坏数据
     */
    public GameRoom getRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(roomKey(roomId));
        if (json == null) {
            return null;
        }
        try {
            // 使用 TypeReference 正确反序列化泛型字段
            return JSON.parseObject(json, new TypeReference<GameRoom>() {
            });
        } catch (Exception e) {
            log.error("解析房间数据失败，自动清理: roomId={}", roomId, e);
            // 清理损坏的房间数据
            cleanupCorruptedRoom(roomId);
            return null;
        }
    }

    /**
     * 清理损坏的房间数据
     */
    private void cleanupCorruptedRoom(String roomId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(roomKey(roomId));
            if (json == null) {
                return;
            }
            // 尝试解析 gameType 以确定索引 key
            JSONObject obj = JSON.parseObject(json);
            if (obj != null) {
                String gameType = obj.getString("gameType");
                stringRedisTemplate.delete(roomKey(roomId));
                stringRedisTemplate.opsForSet().remove(GameRedisKey.getKey(GameRedisKey.ROOMS_ALL), roomId);
                if (gameType != null) {
                    stringRedisTemplate.opsForSet().remove(
                            GameRedisKey.getKey(GameRedisKey.ROOMS_BY_TYPE, gameType),
                            roomId);
                }
            }
        } catch (Exception cleanupEx) {
            log.warn("清理损坏房间失败: roomId={}", roomId, cleanupEx);
        }
    }

    /**
     * 删除房间
     */
    public void deleteRoom(String roomId) {
        if (roomId == null) {
            return;
        }
        GameRoom room = getRoom(roomId);
        stringRedisTemplate.delete(roomKey(roomId));

        // 移除索引
        stringRedisTemplate.opsForSet().remove(GameRedisKey.getKey(GameRedisKey.ROOMS_ALL), roomId);
        if (room != null && room.getGameType() != null) {
            stringRedisTemplate.opsForSet().remove(
                    GameRedisKey.getKey(GameRedisKey.ROOMS_BY_TYPE, room.getGameType().name()),
                    roomId);
        }
    }

    // ==================== 用户-房间映射 ====================

    /**
     * 设置用户-房间映射
     */
    public void putUserRoom(Long userId, String roomId) {
        if (userId == null || roomId == null) {
            return;
        }
        String key = GameRedisKey.getKey(GameRedisKey.USER_ROOM_KEY, userId.toString());
        stringRedisTemplate.opsForValue().set(key, roomId, ROOM_TTL);
    }

    /**
     * 获取用户-房间 ID
     */
    public String getUserRoomId(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = GameRedisKey.getKey(GameRedisKey.USER_ROOM_KEY, userId.toString());
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户-房间映射
     */
    public void removeUserRoom(Long userId) {
        if (userId == null) {
            return;
        }
        String key = GameRedisKey.getKey(GameRedisKey.USER_ROOM_KEY, userId.toString());
        stringRedisTemplate.delete(key);
    }

    // ==================== 房间列表查询 ====================

    /**
     * 获取所有房间
     */
    public List<GameRoom> getAllRooms() {
        Set<String> roomIds = stringRedisTemplate.opsForSet().members(
                GameRedisKey.getKey(GameRedisKey.ROOMS_ALL));
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roomIds.stream()
                .map(this::getRoom)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 按游戏类型获取房间
     */
    public List<GameRoom> getRoomsByType(GameTypeEnum gameType) {
        if (gameType == null) {
            return getAllRooms();
        }
        Set<String> roomIds = stringRedisTemplate.opsForSet().members(
                GameRedisKey.getKey(GameRedisKey.ROOMS_BY_TYPE, gameType.name()));
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roomIds.stream()
                .map(this::getRoom)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有房间 ID
     */
    public Set<String> getAllRoomIds() {
        Set<String> roomIds = stringRedisTemplate.opsForSet().members(
                GameRedisKey.getKey(GameRedisKey.ROOMS_ALL));
        return roomIds != null ? roomIds : Collections.emptySet();
    }

    // ==================== 过期索引 ====================

    /**
     * 设置房间过期时间
     */
    public void setRoomExpiry(String roomId, long expireTime) {
        if (roomId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().add(
                GameRedisKey.getKey(GameRedisKey.ROOMS_EXPIRY),
                roomId,
                expireTime);
    }

    /**
     * 移除房间过期时间
     */
    public void removeRoomExpiry(String roomId) {
        if (roomId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().remove(
                GameRedisKey.getKey(GameRedisKey.ROOMS_EXPIRY),
                roomId);
    }

    /**
     * 获取已过期的房间 ID 集合
     */
    public Set<String> getExpiredRooms(long now) {
        Set<String> expired = stringRedisTemplate.opsForZSet().rangeByScore(
                GameRedisKey.getKey(GameRedisKey.ROOMS_EXPIRY),
                0,
                now);
        return expired != null ? expired : Collections.emptySet();
    }

    /**
     * 获取所有用户-房间映射（用于重启恢复）
     */
    public Map<Long, String> getAllUserRooms() {
        Set<String> keys = stringRedisTemplate.keys(
                GameRedisKey.getKey(GameRedisKey.USER_ROOM_KEY, "*"));
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        for (String key : keys) {
            String roomId = stringRedisTemplate.opsForValue().get(key);
            if (roomId != null) {
                try {
                    String userIdStr = key.substring(key.lastIndexOf(':') + 1);
                    result.put(Long.parseLong(userIdStr), roomId);
                } catch (NumberFormatException e) {
                    log.warn("解析用户ID失败: key={}", key);
                }
            }
        }
        return result;
    }

    // ==================== 工具方法 ====================

    private String roomKey(String roomId) {
        return GameRedisKey.getKey(GameRedisKey.ROOM_KEY, roomId);
    }
}