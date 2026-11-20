package com.cong.fishisland.service.impl.chat;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BigFishSettingsServiceTest {
    @Test
    void defaultsToEnabledAndReadsSavedChoicesFromRedis() {
        RedissonClient redis = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<Boolean> bucket = mock(RBucket.class);
        when(redis.<Boolean>getBucket("chat:big-fish:enabled")).thenReturn(bucket);
        AtomicReference<Boolean> stored = new AtomicReference<>();
        when(bucket.get()).thenAnswer(call -> stored.get());
        doAnswer(call -> {
            stored.set(call.getArgument(0));
            return null;
        }).when(bucket).set(anyBoolean());

        BigFishSettingsService settings = new BigFishSettingsService(redis);
        assertTrue(settings.isEnabled());
        settings.setEnabled(false);
        assertFalse(new BigFishSettingsService(redis).isEnabled());
        settings.setEnabled(true);
        assertTrue(new BigFishSettingsService(redis).isEnabled());
    }
}
