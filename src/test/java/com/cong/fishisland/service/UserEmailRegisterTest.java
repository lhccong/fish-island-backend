package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.user.UserMapper;
import com.cong.fishisland.model.entity.user.EmailBan;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.impl.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 邮箱注册的账号长度边界与自动生成账号行为测试，不连接数据库或 Redis。
 */
@ExtendWith(MockitoExtension.class)
class UserEmailRegisterTest {

    private static final String EMAIL = "register-test@qq.com";

    private static final String PASSWORD = "test-password";

    private static final String CODE = "123456";

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserPointsService userPointsService;

    @Mock
    private EmailBanService emailBanService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    /**
     * 注入继承的持久化入口，使测试只替换外部存储而保留真实注册逻辑。
     */
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    /**
     * 短账号应在注册阶段被拒绝，避免保存后再被登录入口拦截。
     */
    @ParameterizedTest
    @ValueSource(strings = {"L", "Lo", "Lou"})
    void rejectsShortAccountBeforeAccessingExternalServices(String userAccount) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.userEmilRegister(userAccount, EMAIL, PASSWORD, PASSWORD, CODE));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        assertEquals("用户账号过短", exception.getMessage());
        verifyNoInteractions(userMapper, userPointsService, emailBanService,
                redissonClient, stringRedisTemplate);
    }

    /**
     * 四位账号是登录规则允许的下界，邮箱注册应保留该账号。
     */
    @Test
    void registersFourCharacterAccount() {
        prepareSuccessfulRegistration();

        long userId = userService.userEmilRegister("Louu", EMAIL, PASSWORD, PASSWORD, CODE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(123L, userId);
        assertEquals("Louu", userCaptor.getValue().getUserAccount());
        assertEquals(EMAIL, userCaptor.getValue().getEmail());
    }

    /**
     * 未填写账号时继续自动生成账号，避免新增校验破坏已有邮箱注册方式。
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void generatesAccountWhenNotProvided(String userAccount) {
        prepareSuccessfulRegistration();

        long userId = userService.userEmilRegister(userAccount, EMAIL, PASSWORD, PASSWORD, CODE);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(123L, userId);
        assertEquals("邮箱用户register-test@qq.com", userCaptor.getValue().getUserAccount());
    }

    /**
     * 模拟邮箱未占用、未封禁且验证码有效，保留注册和积分初始化的真实调用链。
     */
    @SuppressWarnings("unchecked")
    private void prepareSuccessfulRegistration() {
        LambdaQueryChainWrapper<EmailBan> banQuery = mock(LambdaQueryChainWrapper.class, RETURNS_SELF);
        when(emailBanService.lambdaQuery()).thenReturn(banQuery);
        when(userMapper.selectCount(any())).thenReturn(0L);

        RRateLimiter rateLimiter = mock(RRateLimiter.class);
        when(redissonClient.getRateLimiter(anyString())).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("email:register:lock:" + EMAIL, "1", 10, TimeUnit.SECONDS))
                .thenReturn(true);
        when(valueOperations.get("email:code:" + EMAIL)).thenReturn(CODE);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(123L);
            return 1;
        });
    }
}
