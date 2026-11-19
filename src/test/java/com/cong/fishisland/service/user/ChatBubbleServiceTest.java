package com.cong.fishisland.service.user;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.user.UserChatBubbleMapper;
import com.cong.fishisland.model.entity.user.UserChatBubble;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.vo.user.ChatBubbleVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.impl.user.ChatBubbleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatBubbleServiceTest {
    private static final long ACCOUNT = 123L;
    private static final String STYLE = "berry-cat-paw";
    private final UserChatBubbleMapper mapper = mock(UserChatBubbleMapper.class);
    private final UserPointsService points = mock(UserPointsService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final TransactionStatus status = mock(TransactionStatus.class);
    private final ChatBubbleService service = new ChatBubbleService(mapper, points, transactions);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "chat-bubble-test"), UserPoints.class);
        when(points.getOne(any(LambdaQueryWrapper.class))).thenReturn(new UserPoints());
        when(transactions.getTransaction(any())).thenReturn(status);
        doAnswer(call -> {
            call.<Runnable>getArgument(1).run();
            return null;
        }).when(points).runWithUserPointsLocks(aryEq(new Long[]{ACCOUNT}), any());
    }

    @Test
    void exchangesExpiredGrantForExactlyThirtyDaysAndRecordsDebit() {
        long before = System.currentTimeMillis();
        when(mapper.findExpiry(ACCOUNT, STYLE)).thenReturn(before - 1);
        when(mapper.grant(eq(ACCOUNT), eq(STYLE), anyLong())).thenReturn(2);
        service.exchange(ACCOUNT, STYLE);

        ArgumentCaptor<Long> expiry = ArgumentCaptor.forClass(Long.class);
        verify(mapper).grant(eq(ACCOUNT), eq(STYLE), expiry.capture());
        long period = Duration.ofDays(30).toMillis();
        assertTrue(expiry.getValue() >= before + period);
        assertTrue(expiry.getValue() <= System.currentTimeMillis() + period);
        verify(points).checkAvailablePoints(ACCOUNT, 300);
        verify(points).updateUsedPoints(ACCOUNT, 300, "chat_bubble_exchange", STYLE, "兑换聊天气泡「草莓猫爪」");
        verify(transactions).commit(status);
    }

    @Test
    void blocksDuplicateExchangeBeforeDebitAndRollsBackFailedGrant() {
        when(mapper.findExpiry(ACCOUNT, STYLE)).thenReturn(System.currentTimeMillis() + 60_000);
        assertThrows(BusinessException.class, () -> service.exchange(ACCOUNT, STYLE));
        verify(points, never()).updateUsedPoints(anyLong(), anyInt(), any(), any(), any());
        verify(mapper, never()).grant(anyLong(), any(), anyLong());

        when(mapper.findExpiry(ACCOUNT, STYLE)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.exchange(ACCOUNT, STYLE));
        verify(transactions, times(2)).rollback(status);
        verify(transactions, never()).commit(any());
    }

    @Test
    void expiredStyleCannotBeEquippedAndDefaultClearsSelectionWithoutDebit() {
        assertThrows(BusinessException.class, () -> service.equip(ACCOUNT, STYLE));
        verify(transactions).rollback(status);
        when(mapper.select(eq(ACCOUNT), eq(STYLE), anyLong())).thenReturn(1);
        service.equip(ACCOUNT, STYLE);
        service.equip(ACCOUNT, null);
        verify(mapper, times(3)).clearSelection(ACCOUNT);
        verify(mapper, times(2)).select(eq(ACCOUNT), eq(STYLE), anyLong());
        verify(transactions, times(2)).commit(status);
        verify(points, never()).updateUsedPoints(anyLong(), anyInt(), any(), any(), any());
    }

    @Test
    void catalogKeepsExpiryButNeverShowsExpiredSelectionAsActive() {
        UserChatBubble expired = new UserChatBubble();
        expired.setBubbleId(STYLE);
        expired.setEquipped(1);
        expired.setExpiresAt(System.currentTimeMillis() - 1);
        when(mapper.findAll(ACCOUNT)).thenReturn(Collections.singletonList(expired));
        List<ChatBubbleVO> catalog = service.list(ACCOUNT);
        assertEquals(6, catalog.size());
        assertTrue(catalog.stream().allMatch(item -> item.getPoints() == 300 && item.getDurationDays() == 30));
        ChatBubbleVO catPaw = catalog.stream().filter(item -> STYLE.equals(item.getId())).findFirst().get();
        assertFalse(catPaw.isEquipped());
        assertNotNull(catPaw.getExpiresAt());
        assertNull(service.activeStyle(ACCOUNT));
        assertThrows(BusinessException.class, () -> service.exchange(ACCOUNT, "unknown-bubble"));
        verify(transactions, never()).getTransaction(any());
    }
}
