package com.cong.fishisland.service.impl.chat;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BigFishSettingsService {
    private static final String ENABLED_KEY = "chat:big-fish:enabled";

    private final RedissonClient redis;

    public boolean isEnabled() {
        return !Boolean.FALSE.equals(redis.<Boolean>getBucket(ENABLED_KEY).get());
    }

    public void setEnabled(boolean enabled) {
        redis.<Boolean>getBucket(ENABLED_KEY).set(enabled);
    }
}
