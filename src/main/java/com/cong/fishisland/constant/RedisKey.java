package com.cong.fishisland.constant;

/**
 * Redis 密钥
 *
 * @author cong
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
     * 热帖缓存
     */
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

    /**
     * 用户改名限制 user:rename:{userId}:{yyyyMM}
     */
    String USER_RENAME_LIMIT = "user:rename:%d:%s";

    /**
     * 防止重复提交 key 前缀
     */
    String USER_DEBOUNCE_PREFIX = "user:debounce:";

    /**
     * 用户禁言 user:mute:{userId}
     */
    String USER_MUTE = "user:mute:%d";
    
    /**
     * 管理员撤回消息记录 admin:revoke:records
     */
    String ADMIN_REVOKE_RECORDS = "admin:revoke:records";
    
    String NO_REPEAT_SUBMIT_PREFIX = "noRepeatSubmit:%s:%s";

    /**
     * 用户年度报告内容摘要缓存 annual:report:content:summary:{userId}:{year}
     */
    String ANNUAL_REPORT_CONTENT_SUMMARY = "annual:report:content:summary:%d:%d";

    /**
     * Boss列表缓存
     */
    String BOSS_LIST_CACHE_KEY = "boss:list";

    /**
     * Boss当前血量缓存 boss:health:{bossId}
     */
    String BOSS_HEALTH_CACHE_KEY = "boss:health:%d";

    /**
     * 用户每日Boss挑战记录 boss:battle:user:{userId}:boss:{bossId}:{date}
     */
    String BOSS_BATTLE_USER_DAILY_KEY = "boss:battle:user:%d:boss:%d:%s";

    /**
     * Boss挑战排行榜 boss:ranking:{bossId}
     */
    String BOSS_CHALLENGE_RANKING_KEY = "boss:ranking:%d";

    static String getKey(String key, Object... objects) {
        return BASE_KEY + String.format(key, objects);
    }

}
