package com.cong.fishisland.service.user;

import com.cong.fishisland.mapper.pet.PetTreasureMapper;
import com.cong.fishisland.mapper.user.UserPointsRecordMapper;
import com.cong.fishisland.model.vo.pet.PetTreasureVO;
import com.cong.fishisland.model.vo.user.DailyPointTasksVO;
import com.cong.fishisland.model.vo.user.DailyPointTasksVO.Task;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.TowerClimbService;
import com.cong.fishisland.service.UserSignInService;
import com.cong.fishisland.service.impl.user.DailyPointTasksService;
import com.cong.fishisland.service.pet.PetTreasureService;
import com.cong.fishisland.service.turntable.TurntableDrawRecordService;
import com.cong.fishisland.service.turntable.TurntableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyPointTasksServiceTest {
    @Mock private UserSignInService signIn;
    @Mock private UserPointsRecordMapper records;
    @Mock private PetTreasureMapper companions;
    @Mock private PetTreasureService treasure;
    @Mock private TowerClimbService tower;
    @Mock private FarmLandService farm;
    @Mock private TurntableDrawRecordService draws;
    @Mock private TurntableService turntables;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> values;
    private DailyPointTasksService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        service = new DailyPointTasksService(signIn, records, companions, treasure, tower, farm,
                draws, turntables, redis);
    }

    @Test
    void missingDailyKeysReadAsZeroAndUseTheAuthenticatedAccount() {
        when(values.multiGet(anyList())).thenReturn(Arrays.asList(null, null, null));
        DailyPointTasksVO snapshot = service.snapshot(42L);

        assertEquals("ready", task(snapshot, "sign_in").getState());
        assertEquals(0, task(snapshot, "speak").getProgress().intValue());
        assertEquals(0, task(snapshot, "moments_publish").getProgress().intValue());
        assertEquals(0, task(snapshot, "moments_like").getProgress().intValue());
        assertEquals("conditional", task(snapshot, "pet_treasure").getState());
        assertEquals("conditional", task(snapshot, "farm_harvest").getState());
        assertTrue(snapshot.getRefreshAt() > snapshot.getServerNow());
        verify(values).multiGet(Arrays.asList("user:speak:42:" + LocalDate.now(),
                "user:moments:publish:42:" + LocalDate.now(), "user:moments:like:42:" + LocalDate.now()));
        verify(records).dailyEarned(eq(42L), any(), any());
        verifyNoInteractions(treasure, tower);
    }

    @Test
    void combinesQuotaAndSettledPointsAndRefreshesAtTheEndOfTreasureCooldown() {
        when(values.multiGet(anyList())).thenReturn(Arrays.asList("10", "1", "2"));
        when(signIn.hasSignedOn(eq(42L), any())).thenReturn(true);
        when(companions.companionCount(42L)).thenReturn(1);
        when(farm.hasHarvestableLand(42L)).thenReturn(true);
        when(draws.hasTodayDrawRecord(42L)).thenReturn(true);
        long now = System.currentTimeMillis();
        PetTreasureVO dig = new PetTreasureVO();
        dig.setUsed(2);
        dig.setServerNow(now);
        dig.setResetAt(now + 86400000);
        dig.setNextAvailableAt(now + 90000);
        when(treasure.status(42L)).thenReturn(dig);
        UserPointsRecordMapper.DailyEarned credit = new UserPointsRecordMapper.DailyEarned();
        credit.setSourceType("pet_treasure");
        credit.setEarned(13);
        when(records.dailyEarned(eq(42L), any(), any())).thenReturn(Collections.singletonList(credit));

        DailyPointTasksVO snapshot = service.snapshot(42L);
        assertEquals("done", task(snapshot, "sign_in").getState());
        assertEquals("done", task(snapshot, "speak").getState());
        assertEquals("ready", task(snapshot, "moments_like").getState());
        assertEquals(2, task(snapshot, "moments_like").getProgress().intValue());
        assertEquals("done", task(snapshot, "free_draw").getState());
        assertEquals("ready", task(snapshot, "farm_harvest").getState());
        assertEquals("waiting", task(snapshot, "pet_treasure").getState());
        assertEquals(13, task(snapshot, "pet_treasure").getEarned());
        assertTrue(snapshot.getRefreshAt() <= dig.getNextAvailableAt());
    }

    private Task task(DailyPointTasksVO snapshot, String id) {
        return snapshot.getTasks().stream().filter(task -> task.getId().equals(id)).findFirst().get();
    }
}
