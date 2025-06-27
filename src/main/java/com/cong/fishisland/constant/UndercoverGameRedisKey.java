package com.cong.fishisland.constant;

/**
 * 谁是卧底游戏 Redis 键常量
 *
 * @author cong
 */
public interface UndercoverGameRedisKey {
    
    /**
     * 基础键前缀
     */
    String BASE_KEY = RedisKey.BASE_KEY + "undercover:";
    
    /**
     * 当前活跃房间键
     */
    String ACTIVE_ROOM = BASE_KEY + "active_room";
    
    /**
     * 房间信息键
     */
    String ROOM_INFO = BASE_KEY + "room:%s";
    
    /**
     * 玩家身份键
     */
    String PLAYER_ROLE = BASE_KEY + "player:%d:role";
    
    /**
     * 玩家所在房间键
     */
    String PLAYER_ROOM = BASE_KEY + "player:%d:room";
    
    /**
     * 获取完整的 Redis 键
     *
     * @param key      键模板
     * @param objects  参数
     * @return 完整的 Redis 键
     */
    static String getKey(String key, Object... objects) {
        return String.format(key, objects);
    }
} 