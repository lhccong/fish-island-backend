package com.cong.fishisland.constant;

/**
 * Redis 密钥
 *
 * @author liuhuaicong
 * @date 2023/10/31
 */
public interface RedisKey {
    String BASE_KEY = "fish:";

    /**
     * 在线用户列表
     */
    String ONLINE_UID_ZET = "online";

    /**
     * 离线用户列表
     */
    String OFFLINE_UID_ZET = "offline";

    /**
     * 用户信息
     */
    String USER_INFO_STRING = "userInfo:uid_%d";

    /**
     * 用户token存放
     */
    String USER_TOKEN_STRING = "userToken:uid_%d";

    String HOT_POST_CACHE_KEY = "hot_post_list";

    /**
     * 用户猜对的英雄
     */
    String GUESS_HERO_USER_CURRENT_HERO = "guess:hero:user:%d:current_hero";

    /**
     * 记录猜对英雄次数
     */
    String GUESS_HERO_SUCCESS_COUNT = "guess:hero:success:count";

    /**
     * 猜对英雄排行
     */
    String GUESS_HERO_RANKING = "guess:hero:ranking";

    static String getKey(String key, Object... objects) {
        return BASE_KEY + String.format(key, objects);
    }

}
