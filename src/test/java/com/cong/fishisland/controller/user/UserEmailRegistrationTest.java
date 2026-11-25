package com.cong.fishisland.controller.user;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.user.UserMapper;
import com.cong.fishisland.model.dto.user.UserRegisterRequest;
import com.cong.fishisland.model.entity.user.EmailBan;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.EmailBanService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.impl.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserEmailRegistrationTest {
    private static final String EMAIL = "synthetic-registration@gmail.com";
    private static final String CODE = "123456";

    private final UserController controller = new UserController();
    private final UserMapper mapper = mock(UserMapper.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        UserServiceImpl service = new UserServiceImpl();
        EmailBanService emailBans = mock(EmailBanService.class);
        LambdaQueryChainWrapper<EmailBan> banQuery = mock(LambdaQueryChainWrapper.class);
        when(emailBans.lambdaQuery()).thenReturn(banQuery);
        when(banQuery.eq(any(), any())).thenReturn(banQuery);
        RedissonClient redisson = mock(RedissonClient.class);
        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redisson.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire()).thenReturn(true);

        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "emailBanService", emailBans);
        ReflectionTestUtils.setField(service, "userPointsService", mock(UserPointsService.class));
        ReflectionTestUtils.setField(service, "redissonClient", redisson);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
    }

    @Test
    void savesTrimmedCustomNameOnInsert() {
        User user = register("　岛上的鱼 ");
        assertEquals("岛上的鱼", user.getUserName());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" 　\t"})
    void savesFourDigitDefaultNameWhenNicknameIsBlank(String name) {
        assertTrue(register(name).getUserName().matches("岛民[1-9][0-9]{3}"));
    }

    @Test
    void rejectsOverlongNameBeforeConsumingVerificationCode() {
        BusinessException failure = assertThrows(BusinessException.class,
                () -> controller.userEmailRegister(request("12345678901")));
        assertEquals("用户名不能超过10个字符", failure.getMessage());
        verifyNoInteractions(mapper, redis);
    }

    private User register(String name) {
        when(redis.opsForValue().setIfAbsent("email:register:lock:" + EMAIL, "1", 10, TimeUnit.SECONDS))
                .thenReturn(true);
        when(redis.opsForValue().get("email:code:" + EMAIL)).thenReturn(CODE);
        when(mapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(123L);
            return 1;
        });

        assertEquals(Long.valueOf(123L), controller.userEmailRegister(request(name)).getData());
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(mapper).insert(user.capture());
        return user.getValue();
    }

    private UserRegisterRequest request(String name) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail(EMAIL);
        request.setUserName(name);
        request.setCode(CODE);
        request.setUserPassword("synthetic-password");
        request.setCheckPassword("synthetic-password");
        return request;
    }
}
