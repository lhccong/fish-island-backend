package com.cong.fishisland.constant;

import java.time.LocalDate;

public enum DailyPointCounter {
    SPEAK("user:speak:", 10),
    MOMENTS_PUBLISH("user:moments:publish:", 1),
    MOMENTS_LIKE("user:moments:like:", PointConstant.MAX_DAILY_MOMENTS_LIKE_POINTS);

    private final String prefix;
    public final int limit;

    DailyPointCounter(String prefix, int limit) {
        this.prefix = prefix;
        this.limit = limit;
    }

    public String key(Long userId, LocalDate day) {
        return prefix + userId + ":" + day;
    }

}
