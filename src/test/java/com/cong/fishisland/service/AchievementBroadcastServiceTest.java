package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import com.cong.fishisland.service.admin.BroadcastService;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AchievementBroadcastServiceTest {
    private final BroadcastService sender = mock(BroadcastService.class);
    private final UserService users = mock(UserService.class);
    private final AchievementBroadcastService service = new AchievementBroadcastService(sender, users);

    @AfterEach void clearTransaction() {
        TransactionSynchronizationManager.clear();
    }

    @Test void boundariesAndMessageContents() {
        User user = new User();
        user.setUserName("测试岛民");
        when(users.getById(1L)).thenReturn(user);
        for (int points = 1; points < 10; points++) service.treasureFound(1L, points);
        service.towerCleared(1L, 30);
        service.equipmentUpgraded(1L, EquipSlotEnum.WEAPON, 10);
        service.redEntriesForged(1L, EquipSlotEnum.WEAPON, 0);
        service.sponsorReceived(1L, BigDecimal.ZERO);
        service.sponsorReceived(1L, null);
        verifyNoInteractions(sender, users);

        service.treasureFound(1L, 10);
        service.towerCleared(1L, 31);
        service.towerCleared(1L, 32);
        service.equipmentUpgraded(1L, EquipSlotEnum.WEAPON, 11);
        service.equipmentUpgraded(1L, EquipSlotEnum.SHOES, 12);
        service.redEntriesForged(1L, EquipSlotEnum.WEAPON, 2);
        service.sponsorReceived(1L, new BigDecimal("6.60"));
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(sender, times(7)).publish(messages.capture());
        assertTrue(messages.getAllValues().get(0).contains("测试岛民」在营地挖宝获得 10 积分"));
        assertTrue(messages.getAllValues().get(1).contains("第 31 层"));
        assertTrue(messages.getAllValues().get(2).contains("第 32 层"));
        assertTrue(messages.getAllValues().get(3).contains("武器成功强化至 11 级"));
        assertTrue(messages.getAllValues().get(4).contains("鞋子成功强化至 12 级"));
        assertTrue(messages.getAllValues().get(5).contains("武器锻造出 2 条红词条"));
        assertTrue(messages.getAllValues().get(6).contains("赞助摸鱼岛 6.6 元"));
    }

    @Test void waitsForCommitAndRollbackDoesNotSend() {
        beginTransaction();
        service.treasureFound(1L, 10);
        verifyNoInteractions(sender, users);
        TransactionSynchronization callback = TransactionSynchronizationManager.getSynchronizations().get(0);
        callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(sender, users);
        TransactionSynchronizationManager.clear();

        beginTransaction();
        service.treasureFound(1L, 10);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();
        verify(sender).publish(contains("岛民"));
    }

    @Test void dispatchAndNameLookupFailuresCannotFailCommittedBusiness() {
        beginTransaction();
        doThrow(new IllegalStateException()).when(sender).publish(anyString());
        service.towerCleared(1L, 31);
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit());
        when(users.getById(1L)).thenThrow(new IllegalStateException());
        service.towerCleared(1L, 32);
        assertDoesNotThrow(() -> TransactionSynchronizationManager.getSynchronizations().get(1).afterCommit());
        verify(sender, times(1)).publish(anyString());
    }

    @Test void sanitizesAndBoundsPublicNicknameWithoutUsingAccount() {
        User user = new User();
        user.setUserName(" \u0000\n岛民\t ");
        when(users.getById(1L)).thenReturn(user);
        service.treasureFound(1L, 10);
        verify(sender).publish("恭喜「岛民」在营地挖宝获得 10 积分！");
        user.setUserName(String.join("", java.util.Collections.nCopies(100, "🌊")));
        service.treasureFound(1L, 10);
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(sender, times(2)).publish(messages.capture());
        String message = messages.getAllValues().get(1);
        String nickname = message.substring(message.indexOf('「') + 1, message.indexOf('」'));
        assertEquals(40, nickname.codePointCount(0, nickname.length()));
        assertTrue(message.length() <= 200);
    }

    private void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
}
