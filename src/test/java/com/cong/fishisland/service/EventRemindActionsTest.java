package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.event.EventRemindStateRequest;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.impl.event.EventRemindServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventRemindActionsTest {
    EventRemindServiceImpl service;

    @BeforeEach void setup() {
        service = spy(new EventRemindServiceImpl());
        UserService users = mock(UserService.class);
        User user = new User();
        user.setId(123L);
        when(users.getLoginUser()).thenReturn(user);
        ReflectionTestUtils.setField(service, "userService", users);
    }

    @Test @SuppressWarnings("unchecked") void unreadCountUsesRecipientAndUnreadState() {
        doAnswer(call -> {
            QueryWrapper<EventRemind> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("recipientId ="));
            assertTrue(query.getSqlSegment().contains("state ="));
            assertTrue(query.getParamNameValuePairs().containsValue(123L));
            assertTrue(query.getParamNameValuePairs().containsValue(0));
            return 105L;
        }).when(service).count(any(Wrapper.class));
        assertEquals(105L, service.unreadCount());
    }

    @Test @SuppressWarnings("unchecked") void globalReadIsScopedAndIdempotentWithoutIds() {
        doAnswer(call -> {
            UpdateWrapper<EventRemind> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("recipientId ="));
            assertFalse(sql.contains("id IN"));
            assertTrue(query.getParamNameValuePairs().containsValue(123L));
            assertTrue(query.getSqlSet().contains("state="));
            return false; // zero rows is successful, including a repeat operation
        }).when(service).update(any(Wrapper.class));
        assertTrue(service.batchSetRead());
        assertTrue(service.batchSetRead());
    }

    @Test @SuppressWarnings("unchecked") void selectedReadIncludesIdsAndRejectsEmptySelection() {
        doAnswer(call -> {
            UpdateWrapper<EventRemind> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("recipientId ="));
            assertTrue(query.getSqlSegment().contains("id IN"));
            assertTrue(query.getParamNameValuePairs().containsValue(123L));
            assertTrue(query.getParamNameValuePairs().containsValue(801L));
            return false;
        }).when(service).update(any(Wrapper.class));
        EventRemindStateRequest request = new EventRemindStateRequest();
        request.setIds(Collections.singletonList(801L));
        assertTrue(service.readSelected(request));
        request.setIds(Collections.emptyList());
        assertThrows(BusinessException.class, () -> service.readSelected(request));
    }

    @Test @SuppressWarnings("unchecked") void deleteAllAlwaysScopesRecipientAndEmptyIsSuccessful() {
        doAnswer(call -> {
            QueryWrapper<EventRemind> query = call.getArgument(0);
            assertTrue(query.getSqlSegment().contains("recipientId ="));
            assertFalse(query.getSqlSegment().contains("id IN"));
            assertTrue(query.getParamNameValuePairs().containsValue(123L));
            return false;
        }).when(service).remove(any(Wrapper.class));
        assertTrue(service.deleteAll());
    }

    @Test void scriptDetectionStoresTheDetectedUserAndKeepsTheSystemSender() {
        long detectedUserId = 90071992547409921L;
        doReturn(true).when(service).save(any(EventRemind.class));
        service.sendScriptDetection(123L, detectedUserId, "合成脚本检测");
        ArgumentCaptor<EventRemind> saved = ArgumentCaptor.forClass(EventRemind.class);
        verify(service).save(saved.capture());
        EventRemind notice = saved.getValue();
        assertEquals("script_detection", notice.getAction());
        assertEquals(123L, notice.getRecipientId().longValue());
        assertEquals(detectedUserId, notice.getSourceId().longValue());
        assertEquals(-1L, notice.getSenderId().longValue());
        assertEquals(0, notice.getSourceType().intValue());
        assertEquals(0, notice.getState().intValue());
    }
}
