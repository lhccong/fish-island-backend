package com.cong.fishisland.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.service.impl.farm.FarmLandServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FarmLandWateringTest {

    private FarmLandServiceImpl service;
    private FarmLandMapper lands;
    private FarmPlantRecordMapper records;
    private StpLogic previousLogic;
    private FarmLand land;

    @BeforeEach
    void setUp() {
        previousLogic = StpUtil.stpLogic;
        StpLogic login = mock(StpLogic.class);
        when(login.getLoginIdAsLong()).thenReturn(7L);
        StpUtil.stpLogic = login;
        lands = mock(FarmLandMapper.class);
        records = mock(FarmPlantRecordMapper.class);
        service = new FarmLandServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", lands);
        ReflectionTestUtils.setField(service, "plantRecordMapper", records);
        ReflectionTestUtils.setField(service, "cropMapper", mock(FarmCropMapper.class));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        land = new FarmLand();
        land.setId(1L);
        land.setUserId(7L);
        land.setLocked(0);
        land.setStatus(1);
        land.setPlantedCropId(10L);
        land.setPlantedTime(now.minusHours(1));
        land.setHarvestTime(now.plusHours(5));
        when(lands.selectById(1L)).thenReturn(land);
        when(lands.waterIfAllowed(any(), any(), any(), any())).thenReturn(1);
        when(records.updateHarvestTimeAfterWatering(anyLong(), any(), any())).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        StpUtil.stpLogic = previousLogic;
    }

    @Test
    void firstWaterReducesRemainingTimeAndSynchronizesRecord() {
        LocalDateTime original = land.getHarvestTime();
        FarmLand result = service.waterLand(1L);
        long remaining = Duration.between(result.getLastWateredTime(), original).getSeconds();
        assertEquals(original.minusSeconds((remaining * 2 + 99) / 100), result.getHarvestTime());
        verify(records).updateHarvestTimeAfterWatering(1L, original, result.getHarvestTime());
        verify(lands).waterIfAllowed(land, result.getHarvestTime(), result.getLastWateredTime(),
                result.getLastWateredTime().minusMinutes(30));
        LandDTO dto = service.toDTO(result);
        assertEquals(result.getLastWateredTime(), dto.getLastWateredTime());
        assertEquals(result.getLastWateredTime().plusMinutes(30), dto.getNextWaterTime());
    }

    @Test
    void repeatedWaterInsideCooldownDoesNotWrite() {
        land.setLastWateredTime(LocalDateTime.now().minusMinutes(29));
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        verify(lands, never()).waterIfAllowed(any(), any(), any(), any());
        verifyNoInteractions(records);
    }

    @Test
    void waterAllowedAfterThirtyMinutes() {
        land.setLastWateredTime(LocalDateTime.now().withNano(0).minusMinutes(30));
        assertNotNull(service.waterLand(1L).getLastWateredTime());
        verify(records).updateHarvestTimeAfterWatering(eq(1L), any(), any());
    }

    @Test
    void rejectsOtherUsersLand() {
        land.setUserId(8L);
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        verify(lands, never()).waterIfAllowed(any(), any(), any(), any());
    }

    @Test
    void rejectsLockedIdleAndMaturePlants() {
        land.setLocked(1);
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        land.setLocked(0);
        land.setStatus(0);
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        land.setStatus(2);
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        land.setStatus(1);
        land.setHarvestTime(LocalDateTime.now().minusSeconds(1));
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        verify(lands, never()).waterIfAllowed(any(), any(), any(), any());
    }

    @Test
    void rejectsMissingIdAndMissingLand() {
        assertThrows(BusinessException.class, () -> service.waterLand(null));
        assertThrows(BusinessException.class, () -> service.waterLand(2L));
        verifyNoInteractions(records);
    }

    @Test
    void concurrentUpdateFailureDoesNotUpdateRecord() {
        when(lands.waterIfAllowed(any(), any(), any(), any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        assertNull(land.getLastWateredTime());
        verifyNoInteractions(records);
    }

    @Test
    void missingOrChangedRecordFailsTransaction() {
        when(records.updateHarvestTimeAfterWatering(anyLong(), any(), any())).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
        assertNull(land.getLastWateredTime());
    }

    @Test
    void wateringOnePlantDoesNotBlockAnother() {
        service.waterLand(1L);
        FarmLand other = new FarmLand();
        other.setId(2L);
        other.setUserId(7L);
        other.setLocked(0);
        other.setStatus(1);
        other.setPlantedCropId(10L);
        other.setPlantedTime(land.getPlantedTime());
        other.setHarvestTime(land.getHarvestTime());
        when(lands.selectById(2L)).thenReturn(other);
        assertNotNull(service.waterLand(2L).getLastWateredTime());
        assertThrows(BusinessException.class, () -> service.waterLand(1L));
    }

    @Test
    void matureLandHasNoNextWaterTime() {
        land.setHarvestTime(LocalDateTime.now().minusSeconds(1));
        land.setLastWateredTime(LocalDateTime.now().minusMinutes(1));
        assertNull(service.toDTO(land).getNextWaterTime());
    }
}
