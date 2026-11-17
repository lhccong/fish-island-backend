package com.cong.fishisland.service.user;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.service.UserPointsRecordService;
import com.cong.fishisland.service.impl.user.UserPointsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserPointsUsedPointsTest {
    private UserPointsServiceImpl service;
    private UserPointsRecordService records;
    private UserPoints account;

    @BeforeEach
    void setup() {
        service = spy(new UserPointsServiceImpl());
        records = mock(UserPointsRecordService.class);
        ReflectionTestUtils.setField(service, "userPointsRecordService", records);
        doAnswer(i -> { ((Runnable) i.getArgument(1)).run(); return null; })
                .when(service).runWithUserPointsLocks(any(), any());
        account = new UserPoints();
        account.setUserId(1L);
        account.setPoints(100);
        account.setUsedPoints(20);
        account.setLevel(3);
        doReturn(account).when(service).getById(1L);
        doReturn(true).when(service).updateById(any(UserPoints.class));
        when(records.addPointsRecord(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void negativeRewardIncreasesAvailablePointsAndWritesOneAttributedLedgerEntry() {
        service.updateUsedPoints(1L, -7, "pet_treasure", "dig", "营地挖宝奖励");
        assertEquals(13, account.getUsedPoints());
        assertEquals(87, account.getPoints() - account.getUsedPoints());
        assertEquals(100, account.getPoints());
        assertEquals(3, account.getLevel());
        verify(service, times(1)).updateById(account);
        verify(records, times(1)).addPointsRecord(1L, 1, 7, 100, 100, 20, 13,
                "pet_treasure", "dig", "营地挖宝奖励");
    }

    @Test
    void rewardAlsoWorksWithNullOrZeroUsedPoints() {
        account.setUsedPoints(null);
        service.updateUsedPoints(1L, -7, "pet_treasure", "dig", "营地挖宝奖励");
        assertEquals(-7, account.getUsedPoints());
        assertEquals(107, account.getPoints() - account.getUsedPoints());
        verify(records).addPointsRecord(1L, 1, 7, 100, 100, 0, -7,
                "pet_treasure", "dig", "营地挖宝奖励");
    }

    @Test
    void positiveChangesStillConsumeAvailablePointsAndRecordTheExpense() {
        service.updateUsedPoints(1L, 5, "pet_feed", "feed", "喂食");
        assertEquals(25, account.getUsedPoints());
        verify(records).addPointsRecord(1L, 2, 5, 100, 100, 20, 25, "pet_feed", "feed", "喂食");
    }

    @Test
    void failedBalanceUpdateThrowsBeforeWritingLedger() {
        doReturn(false).when(service).updateById(any(UserPoints.class));
        assertThrows(BusinessException.class,
                () -> service.updateUsedPoints(1L, -7, "pet_treasure", "dig", "营地挖宝奖励"));
        verifyNoInteractions(records);
    }

    @Test
    void failedRewardLedgerThrowsSoTheCallerTransactionCanRollBack() {
        when(records.addPointsRecord(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyString(), anyString(), anyString())).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> service.updateUsedPoints(1L, -7, "pet_treasure", "dig", "营地挖宝奖励"));
    }

    @Test
    void failedExpenseLedgerAlsoThrows() {
        when(records.addPointsRecord(anyLong(), anyInt(), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyString(), anyString(), anyString())).thenReturn(false);
        assertThrows(BusinessException.class,
                () -> service.updateUsedPoints(1L, 5, "pet_feed", "feed", "喂食"));
    }
}
