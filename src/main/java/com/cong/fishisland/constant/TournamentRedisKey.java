package com.cong.fishisland.constant;

/**
 * 宠物武道大会 Redis 键常量
 *
 * @author cong
 */
public interface TournamentRedisKey {

    String BASE_KEY = "fish:tournament:";

    /**
     * 排行榜 ZSet，score = 坑位分值（越大排名越靠前）
     * key: fish:tournament:leaderboard
     * member: userId, score: (MAX_RANK - rank + 1)
     */
    String LEADERBOARD = BASE_KEY + "leaderboard";

    /**
     * 坑位占用信息 Hash，field = rank（名次字符串），value = userId
     * key: fish:tournament:slots
     */
    String SLOTS = BASE_KEY + "slots";

    /**
     * 每日挑战冷却，防止同一对手重复刷（String，TTL到当天结束）
     * key: fish:tournament:cd:{userId}:{opponentUserId}
     */
    String CHALLENGE_CD_PREFIX = BASE_KEY + "cd:";

    static String challengeCdKey(long userId, long opponentUserId) {
        return CHALLENGE_CD_PREFIX + userId + ":" + opponentUserId;
    }
}
