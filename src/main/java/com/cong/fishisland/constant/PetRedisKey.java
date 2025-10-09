package com.cong.fishisland.constant;

/**
 * 宠物相关Redis键
 *
 * @author cong
 */
public interface PetRedisKey {

    /**
     * 基础键前缀
     */
    String BASE_KEY = "fish:pet:";

    /**
     * 宠物排行榜
     */
    String PET_RANK = "rank";
    
    /**
     * 宠物达到30级的时间记录
     */
    String PET_LEVEL_30_TIME = "level30time";

    /**
     * 获取完整的Redis键
     *
     * @param keys 键的各个部分
     * @return 完整的Redis键
     */
    static String getKey(String... keys) {
        StringBuilder sb = new StringBuilder(BASE_KEY);
        for (String key : keys) {
            sb.append(key).append(":");
        }
        // 移除最后一个冒号
        if (sb.charAt(sb.length() - 1) == ':') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
} 