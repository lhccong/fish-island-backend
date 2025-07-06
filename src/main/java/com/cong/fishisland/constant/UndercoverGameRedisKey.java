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
    String ROOM_INFO = BASE_KEY + "roomInfo:%s";
    
    /**
     * 玩家身份键
     */
    String PLAYER_ROLE = BASE_KEY + "player:%d:role";
    
    /**
     * 玩家所在房间键
     */
    String PLAYER_ROOM = BASE_KEY + "player:%d:room";
    
    /**
     * 房间投票记录键
     */
    String ROOM_VOTES = BASE_KEY + "room:%s:votes";
    
    /**
     * 房间投票计数键（记录每个玩家收到的票数）
     */
    String ROOM_VOTE_COUNT = BASE_KEY + "room:%s:vote_count";
    
    /**
     * 玩家投票状态键（记录玩家是否已投票）
     */
    String PLAYER_VOTED = BASE_KEY + "room:%s:player:%d:voted";
    
    /**
     * 游戏结果键
     */
    String ROOM_RESULT = BASE_KEY + "room:%s:result";
    
    /**
     * 已使用词语对键（记录当天已使用的词语对）
     */
    String USED_WORD_PAIRS = BASE_KEY + "used_word_pairs";
    
    /**
     * 卧底猜词次数键（记录卧底已猜词次数）
     */
    String PLAYER_GUESS_COUNT = BASE_KEY + "room:%s:player:%d:guess_count";
    
    /**
     * 卧底猜词最大次数
     */
    int MAX_GUESS_COUNT = 3;
    
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