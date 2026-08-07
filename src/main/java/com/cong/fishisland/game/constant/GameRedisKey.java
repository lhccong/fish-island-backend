package com.cong.fishisland.game.constant;

/**
 * 游戏模块 Redis Key 常量
 *
 * @author cong
 */
public interface GameRedisKey {

    String BASE_KEY = "fish:game:";

    /**
     * 房间数据 room:{roomId}
     */
    String ROOM_KEY = "room:%s";

    /**
     * 所有房间 ID 集合 rooms:all
     */
    String ROOMS_ALL = "rooms:all";

    /**
     * 按游戏类型索引 rooms:type:{gameType}
     */
    String ROOMS_BY_TYPE = "rooms:type:%s";

    /**
     * 等待中的房间索引 rooms:waiting
     */
    String ROOMS_WAITING = "rooms:waiting";

    /**
     * 用户-房间映射 user:room:{userId}
     */
    String USER_ROOM_KEY = "user:room:%s";

    /**
     * 用户会话 user:session:{userId}
     */
    String USER_SESSION_KEY = "user:session:%s";

    /**
     * 房间过期时间索引 rooms:expiry
     */
    String ROOMS_EXPIRY = "rooms:expiry";

    static String getKey(String key, Object... args) {
        return BASE_KEY + String.format(key, args);
    }
}