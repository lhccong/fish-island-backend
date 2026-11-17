package com.cong.fishisland.service;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.PetForgeProperties;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.entity.pet.*;
import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import com.cong.fishisland.service.impl.pet.PetEquipForgeServiceImpl;
import org.junit.jupiter.api.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class PetForgeBroadcastTest {
    private final AchievementBroadcastService broadcasts = mock(AchievementBroadcastService.class);
    private final FishPetService pets = mock(FishPetService.class);
    private final UserPointsService points = mock(UserPointsService.class);
    private final RedissonClient redis = mock(RedissonClient.class);
    private final Random random = mock(Random.class);
    private final PetEquipForge forge = new PetEquipForge();
    private PetEquipForgeServiceImpl service;
    private StpLogic previous;

    @BeforeEach void setup() throws Exception {
        previous = StpUtil.getStpLogic();
        StpLogic login = mock(StpLogic.class);
        when(login.getLoginType()).thenReturn("login");
        when(login.getLoginIdAsLong()).thenReturn(1L);
        StpUtil.setStpLogic(login);
        service = spy(new PetEquipForgeServiceImpl(pets, points, new PetForgeProperties(), redis, broadcasts));
        ReflectionTestUtils.setField(service, "random", random);
        FishPet pet = new FishPet();
        pet.setPetId(2L); pet.setUserId(1L); pet.setIsDelete(0);
        when(pets.getById(2L)).thenReturn(pet);
        forge.setEquipSlot(1); forge.setEquipLevel(10);
        LambdaQueryChainWrapper<PetEquipForge> query = mock(LambdaQueryChainWrapper.class);
        doReturn(query).when(service).lambdaQuery();
        when(query.eq(any(), any())).thenReturn(query);
        when(query.one()).thenReturn(forge);
        doReturn(true).when(service).updateById(forge);
        RLock lock = mock(RLock.class);
        when(redis.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
    }

    @AfterEach void restoreLogin() { StpUtil.setStpLogic(previous); }

    @Test void successNotFailureTriggersUpgradeAndWriteFailureCannotBroadcast() {
        ForgeUpgradeRequest request = new ForgeUpgradeRequest();
        request.setPetId(2L); request.setEquipSlot(1);
        when(random.nextInt(100)).thenReturn(0);
        assertTrue(service.upgradeEquip(request));
        verify(broadcasts).equipmentUpgraded(1L, EquipSlotEnum.WEAPON, 11);
        clearInvocations(broadcasts);
        when(random.nextInt(100)).thenReturn(99);
        assertFalse(service.upgradeEquip(request));
        verifyNoInteractions(broadcasts);
        when(random.nextInt(100)).thenReturn(0);
        doReturn(false).when(service).updateById(forge);
        assertThrows(BusinessException.class, () -> service.upgradeEquip(request));
        verifyNoInteractions(broadcasts);
    }

    @Test void countsOnlyFreshRedEntriesAndAggregatesOneRefresh() {
        forge.setEntry1(new EquipEntry("attack", 5, 1.0, true));
        ForgeRefreshRequest request = new ForgeRefreshRequest();
        request.setPetId(2L); request.setEquipSlot(1);
        when(random.nextInt(100)).thenReturn(99);
        service.refreshEntries(request);
        verify(broadcasts).redEntriesForged(1L, EquipSlotEnum.WEAPON, 3);
        assertTrue(forge.getEntry1().getLocked());
        clearInvocations(broadcasts);
        when(random.nextInt(100)).thenReturn(0);
        service.refreshEntries(request);
        verify(broadcasts).redEntriesForged(1L, EquipSlotEnum.WEAPON, 0);
    }

    @Test void failedRefreshWriteCannotBroadcast() {
        ForgeRefreshRequest request = new ForgeRefreshRequest();
        request.setPetId(2L); request.setEquipSlot(1);
        when(random.nextInt(100)).thenReturn(99);
        doReturn(false).when(service).updateById(forge);
        assertThrows(BusinessException.class, () -> service.refreshEntries(request));
        verifyNoInteractions(broadcasts);
    }
}
