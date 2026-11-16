package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.manager.EmailManager;
import com.cong.fishisland.model.entity.user.EmailBan;
import com.cong.fishisland.service.impl.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserEmailSendTest {
    private static final String EMAIL = "synthetic-email-send-test@gmail.com";
    private static final String KEY = "email:code:" + EMAIL;
    private UserServiceImpl service;
    private ValueOperations<String, String> values;
    private EmailManager mail;
    private MockHttpServletRequest request;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        service = new UserServiceImpl();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        mail = mock(EmailManager.class);
        EmailBanService bans = mock(EmailBanService.class);
        LambdaQueryChainWrapper<EmailBan> query = mock(LambdaQueryChainWrapper.class);
        when(bans.lambdaQuery()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.exists()).thenReturn(false);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "emailManager", mail);
        ReflectionTestUtils.setField(service, "emailBanService", bans);
        request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
    }

    @Test
    void firstRequestSendsAndRepeatRequestIsRejected() {
        when(values.setIfAbsent(KEY, "SENT", Duration.ofMinutes(5)))
                .thenReturn(true, false);

        assertTrue(service.userEmailSend(EMAIL, request));
        BusinessException failure = assertThrows(BusinessException.class,
                () -> service.userEmailSend(EMAIL, request));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), failure.getCode());
        assertEquals("验证码已发送，请稍后再试", failure.getMessage());
        verify(mail, times(1)).sendVerificationCode(EMAIL);
    }

    @Test
    void existingCodeIsNotOverwrittenOrSentAgain() {
        when(values.setIfAbsent(KEY, "SENT", Duration.ofMinutes(5))).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.userEmailSend(EMAIL, request));

        verifyNoInteractions(mail);
    }

    @Test
    void missingReservationConfirmationDoesNotSend() {
        when(values.setIfAbsent(KEY, "SENT", Duration.ofMinutes(5))).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.userEmailSend(EMAIL, request));

        verifyNoInteractions(mail);
    }
}
