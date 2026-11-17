package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.mapper.pet.PetTreasureMapper;
import com.cong.fishisland.model.entity.pet.PetTreasureDig;
import com.cong.fishisland.model.vo.pet.PetTreasureVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PetTreasureServiceTest {
    private final List<PetTreasureDig> digs = new ArrayList<>();
    private final MutableClock clock = new MutableClock();
    private PetTreasureMapper mapper;
    private PlatformTransactionManager manager;
    private PetTreasureService service;
    private UserPointsService points;
    private AchievementBroadcastService broadcasts;

    @BeforeEach
    void setup() {
        mapper = mock(PetTreasureMapper.class);
        manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        points = mock(UserPointsService.class);
        doAnswer(i -> { ((Runnable) i.getArgument(1)).run(); return null; })
                .when(points).runWithUserPointsLocks(any(), any());
        when(mapper.latest(anyLong())).thenAnswer(i -> digs.isEmpty() ? null : digs.get(digs.size() - 1));
        when(mapper.countDay(anyLong(), any())).thenAnswer(i -> (int) digs.stream()
                .filter(d -> d.getDigDay().equals(i.getArgument(1))).count());
        when(mapper.companionCount(1L)).thenReturn(1);
        when(mapper.insert(any())).thenAnswer(i -> { digs.add(i.getArgument(0)); return 1; });
        when(mapper.owned(anyLong(), anyString())).thenAnswer(i -> digs.stream()
                .filter(d -> d.getUserId().equals(i.getArgument(0)) && d.getId().equals(i.getArgument(1)))
                .findFirst().orElse(null));
        when(mapper.complete(anyLong(), anyString(), anyLong())).thenAnswer(i -> {
            PetTreasureDig d = digs.stream().filter(dig -> dig.getId().equals(i.getArgument(1))).findFirst().get();
            if (d.getCompletedAt() != null) return 0;
            d.setCompletedAt(i.getArgument(2)); return 1;
        });
        broadcasts = mock(AchievementBroadcastService.class);
        service = new PetTreasureService(mapper, points, new TransactionTemplate(manager), clock, broadcasts);
    }

    @Test
    void fiveSecondsAndIdempotencyAreEnforcedByServer() {
        PetTreasureVO started = service.start(1L, "grove");
        String id = started.getActive().getId();
        assertNull(started.getLastReward());
        assertEquals(id, service.start(1L, "brook").getActive().getId());
        assertThrows(BusinessException.class, () -> service.finish(1L, id));
        clock.now += 4999;
        assertThrows(BusinessException.class, () -> service.finish(1L, id));
        clock.now++;
        PetTreasureVO done = service.finish(1L, id);
        assertNull(done.getActive());
        assertTrue(done.getLastReward().getPoints() >= 1 && done.getLastReward().getPoints() <= 10);
        service.finish(1L, id);
        verify(points, times(1)).updateUsedPoints(1L, -done.getLastReward().getPoints(),
                "pet_treasure", id, "营地挖宝奖励");
        assertEquals(1, digs.size());
    }

    @Test
    void hourlyRefreshAndFiveDailyAttemptsSurviveNewServiceInstances() {
        for (int n = 0; n < 5; n++) {
            String id = service.start(1L, n % 2 == 0 ? "flowers" : "path").getActive().getId();
            clock.now += PetTreasureService.DURATION;
            service.finish(1L, id);
            assertThrows(BusinessException.class, () -> service.start(1L, "brook"));
            clock.now += PetTreasureService.REFRESH;
        }
        PetTreasureService reloaded = new PetTreasureService(mapper, mock(UserPointsService.class),
                new TransactionTemplate(manager), clock, broadcasts);
        assertEquals(5, reloaded.status(1L).getUsed());
        assertThrows(BusinessException.class, () -> service.start(1L, "clearing"));
        clock.now = reloaded.status(1L).getResetAt();
        assertEquals(0, service.status(1L).getUsed());
        assertNotNull(service.start(1L, "clearing").getActive());
    }

    @Test
    void midnightKeepsPendingDigAndCooldownAndChecksOwnership() {
        clock.now = Instant.parse("2026-11-16T15:59:58Z").toEpochMilli();
        String id = service.start(1L, "brook").getActive().getId();
        clock.now += 5000;
        assertEquals(0, service.status(1L).getUsed());
        assertEquals(id, service.status(1L).getActive().getId());
        assertThrows(BusinessException.class, () -> service.finish(2L, id));
        service.finish(1L, id);
        assertThrows(BusinessException.class, () -> service.start(1L, "path"));
        clock.now += PetTreasureService.REFRESH;
        assertNotNull(service.start(1L, "path").getActive());
    }

    @Test
    void invalidSitesNoCompanionAndPointFailureRollsBackAndCanRetry() {
        assertThrows(BusinessException.class, () -> service.start(1L, "lake"));
        assertThrows(BusinessException.class, () -> service.start(2L, "grove"));
        String id = service.start(1L, "grove").getActive().getId();
        clock.now += 5000;
        doThrow(new BusinessException(ErrorCode.OPERATION_ERROR, "积分服务失败"))
                .doNothing().when(points).updateUsedPoints(eq(1L), anyInt(), eq("pet_treasure"), eq(id), eq("营地挖宝奖励"));
        // Simulate the completion flag restoration performed by the real database transaction.
        doAnswer(i -> { digs.get(0).setCompletedAt(null); return null; }).when(manager).rollback(any());
        clearInvocations(manager);
        assertThrows(BusinessException.class, () -> service.finish(1L, id));
        verify(manager, times(1)).rollback(any());
        assertNull(digs.get(0).getCompletedAt());
        verifyNoInteractions(broadcasts);
        PetTreasureVO done = service.finish(1L, id);
        assertNotNull(done.getLastReward());
        service.finish(1L, id);
        verify(points, times(2)).updateUsedPoints(1L, -done.getLastReward().getPoints(),
                "pet_treasure", id, "营地挖宝奖励");
        verify(broadcasts, times(1)).treasureFound(1L, done.getLastReward().getPoints());
    }

    @Test
    void onlyWinningSettlementTriggersBroadcastAndRetryDoesNotRepeatIt() {
        String id = service.start(1L, "grove").getActive().getId();
        digs.get(0).setReward(10);
        clock.now += 5000;
        service.finish(1L, id);
        service.finish(1L, id);
        verify(broadcasts, times(1)).treasureFound(1L, 10);
        org.mockito.InOrder order = inOrder(mapper, points, broadcasts, manager);
        order.verify(mapper).complete(eq(1L), eq(id), anyLong());
        order.verify(points).updateUsedPoints(1L, -10, "pet_treasure", id, "营地挖宝奖励");
        order.verify(broadcasts).treasureFound(1L, 10);
        order.verify(manager).commit(any());
    }

    @Test
    void lostConditionalSettlementDoesNotBroadcastOrCredit() {
        String id = service.start(1L, "grove").getActive().getId();
        digs.get(0).setReward(10);
        clock.now += 5000;
        doReturn(0).when(mapper).complete(anyLong(), anyString(), anyLong());
        service.finish(1L, id);
        verifyNoInteractions(broadcasts);
        verify(points, never()).updateUsedPoints(anyLong(), anyInt(), anyString(), anyString(), anyString());
    }

    private static class MutableClock extends Clock {
        long now = Instant.parse("2026-11-16T01:00:00Z").toEpochMilli();
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
    }
}
