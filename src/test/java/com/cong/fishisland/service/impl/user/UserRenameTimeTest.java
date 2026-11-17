package com.cong.fishisland.service.impl.user;

import com.cong.fishisland.model.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRenameTimeTest {
    private UserServiceImpl service;
    private StringRedisTemplate redis;
    private User user;

    @BeforeEach void setup() {
        service = new UserServiceImpl();
        redis = mock(StringRedisTemplate.class);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        user = new User();
        user.setId(123L);
        user.setUserName("合成岛友");
        user.setUserRole("user");
    }

    @Test void usesCurrentCalendarMonthAndReturnsNextMonthMidnightIncludingYearAndLeapBoundaries() {
        for (String[] example : new String[][]{
                {"2026-11-17T12:30:00+08:00", "fish:user:rename:123:202611", "2026-12-01T00:00:00+08:00"},
                {"2026-12-31T23:59:59+08:00", "fish:user:rename:123:202612", "2027-01-01T00:00:00+08:00"},
                {"2028-02-29T12:00:00+08:00", "fish:user:rename:123:202802", "2028-03-01T00:00:00+08:00"}
        }) {
            when(redis.hasKey(example[1])).thenReturn(true);
            assertEquals(example[2], service.nextRenameTime(user, ZonedDateTime.parse(example[0])));
            verify(redis).hasKey(example[1]);
        }
    }

    @Test void availableUsersAndExistingExemptionsHaveNoFutureBoundary() {
        ZonedDateTime now = ZonedDateTime.parse("2026-11-17T12:00:00+08:00");
        assertNull(service.nextRenameTime(user, now));
        clearInvocations(redis);
        user.setUserName("");
        assertNull(service.nextRenameTime(user, now));
        user.setUserName("合成岛友");
        for (String role : new String[]{"admin", "vip", "ban"}) {
            user.setUserRole(role);
            assertNull(service.nextRenameTime(user, now));
        }
        verifyNoInteractions(redis);
    }
}
