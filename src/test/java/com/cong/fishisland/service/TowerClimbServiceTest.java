package com.cong.fishisland.service;

import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.game.TowerClimbProgressMapper;
import com.cong.fishisland.mapper.game.TowerClimbRecordMapper;
import com.cong.fishisland.model.entity.game.TowerClimbProgress;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import org.springframework.data.redis.core.ValueOperations;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.TowerClimbResultVO;
import com.cong.fishisland.model.vo.game.TowerFloorMonsterVO;
import com.cong.fishisland.model.vo.game.TowerProgressVO;
import com.cong.fishisland.service.impl.game.TowerClimbServiceImpl;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TowerClimbServiceTest {

    private final UserService userService = mock(UserService.class);
    private final FishPetService petService = mock(FishPetService.class);
    private final UserPointsService pointsService = mock(UserPointsService.class);
    private final TowerClimbRecordMapper recordMapper = mock(TowerClimbRecordMapper.class);
    private final TowerClimbProgressMapper progressMapper = mock(TowerClimbProgressMapper.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final AchievementBroadcastService broadcasts = mock(AchievementBroadcastService.class);
    private final TowerClimbServiceImpl service = new TowerClimbServiceImpl(
            userService, petService, pointsService, recordMapper, progressMapper, redisTemplate, broadcasts);

    @Test
    void rewardsScaleThroughFloor43() {
        assertEquals(10, service.getFloorMonster(1).getRewardPoints());
        assertEquals(420, service.getFloorMonster(42).getRewardPoints());
        assertEquals(430, service.getFloorMonster(43).getRewardPoints());
    }

    @Test
    void rewardsAreFixedAfterFloor43() {
        assertEquals(100, service.getFloorMonster(44).getRewardPoints());
        assertEquals(100, service.getFloorMonster(45).getRewardPoints());
        assertEquals(100, service.getFloorMonster(66).getRewardPoints());
    }

    @Test
    void statsRemainUnchangedThroughFloor43() {
        for (int floor = 1; floor <= 43; floor++) {
            TowerFloorMonsterVO monster = service.getFloorMonster(floor);
            assertEquals((int) (200 * Math.pow(floor, 1.4)), monster.getHealth());
            assertEquals((int) (15 * Math.pow(floor, 1.3)), monster.getAttack());
            assertEquals(0, monster.getDefense());
            assertEquals(0, monster.getSpeed());
            assertResistances(monster, 0.0);
        }
    }

    @Test
    void highFloorStatsAreReducedWithoutChangingSpecialAttributesOrRewards() {
        for (int floor = 44; floor <= 66; floor++) {
            TowerFloorMonsterVO monster = service.getFloorMonster(floor);
            assertEquals((int) (200 * Math.pow(floor, 1.4) * 0.64), monster.getHealth());
            assertEquals((int) (15 * Math.pow(floor, 1.3) * 0.64), monster.getAttack());
            assertEquals((floor - 3) * 0.006, monster.getCritRate(), 0.000001);
            assertEquals((floor - 5) * 0.006, monster.getComboRate(), 0.000001);
            assertEquals((floor - 8) * 0.006, monster.getDodgeRate(), 0.000001);
            assertEquals((floor - 4) * 0.006, monster.getBlockRate(), 0.000001);
            assertEquals((floor - 6) * 0.006, monster.getLifesteal(), 0.000001);
            assertEquals(100, monster.getRewardPoints());
        }
        assertEquals(25588, service.getFloorMonster(44).getHealth());
        assertEquals(1314, service.getFloorMonster(44).getAttack());
    }

    @Test
    void advancedStatsGrowFromFloor44Through66() {
        for (int floor = 44; floor <= 66; floor++) {
            TowerFloorMonsterVO monster = service.getFloorMonster(floor);
            assertEquals(50 + (floor - 44) * 5, monster.getDefense());
            assertEquals(5 + (floor - 44), monster.getSpeed());
            assertResistances(monster, 0.05 + (floor - 44) * 0.005);
        }
    }

    @Test
    void battleUsesAllDisplayedMonsterStats() {
        for (int floor = 1; floor <= 66; floor++) {
            TowerFloorMonsterVO monster = service.getFloorMonster(floor);
            BattleStatsVO stats = ReflectionTestUtils.invokeMethod(service, "buildMonsterStats", monster);
            assertNotNull(stats);
            assertEquals(monster.getHealth().intValue(), stats.getHealth());
            assertEquals(monster.getAttack().intValue(), stats.getAttack());
            assertEquals(monster.getDefense().intValue(), stats.getDefense());
            assertEquals(monster.getSpeed().intValue(), stats.getSpeed());
            assertEquals(monster.getCritResistance(), stats.getCritResistance(), 0.000001);
            assertEquals(monster.getComboResistance(), stats.getComboResistance(), 0.000001);
            assertEquals(monster.getDodgeResistance(), stats.getDodgeResistance(), 0.000001);
            assertEquals(monster.getBlockResistance(), stats.getBlockResistance(), 0.000001);
            assertEquals(monster.getLifestealResistance(), stats.getLifestealResistance(), 0.000001);
        }
    }

    private void assertResistances(TowerFloorMonsterVO monster, double expected) {
        assertEquals(expected, monster.getCritResistance(), 0.000001);
        assertEquals(expected, monster.getComboResistance(), 0.000001);
        assertEquals(expected, monster.getDodgeResistance(), 0.000001);
        assertEquals(expected, monster.getBlockResistance(), 0.000001);
        assertEquals(expected, monster.getLifestealResistance(), 0.000001);
    }

    @Test
    void battleAllowsFiftyTotalActionsNotFiftyPerSide() {
        BattleStatsVO pet = durableFighter();
        BattleStatsVO monster = durableFighter();
        pet.setSpeed(1);
        List<BattleResultVO> rounds = ReflectionTestUtils.invokeMethod(service, "doBattle", pet, monster);
        assertNotNull(rounds);
        assertEquals(50, rounds.size());
        assertEquals(25L, rounds.stream().filter(round -> "PET".equals(round.getAttackerType())).count());
        assertEquals(25L, rounds.stream().filter(round -> "BOSS".equals(round.getAttackerType())).count());
    }

    @Test
    void comboAndDodgedAttacksStillConsumeActions() {
        BattleStatsVO pet = durableFighter();
        BattleStatsVO monster = durableFighter();
        pet.setSpeed(1);
        pet.setComboRate(1.0);
        List<BattleResultVO> combos = ReflectionTestUtils.invokeMethod(service, "doBattle", pet, monster);
        assertNotNull(combos);
        assertEquals(50, combos.size());
        assertTrue(combos.stream().allMatch(round -> "PET".equals(round.getAttackerType())));

        pet.setDodgeRate(1.0);
        monster.setDodgeRate(1.0);
        List<BattleResultVO> dodges = ReflectionTestUtils.invokeMethod(service, "doBattle", pet, monster);
        assertNotNull(dodges);
        assertEquals(50, dodges.size());
        assertTrue(dodges.stream().allMatch(round -> round.getDamage() == 0));
    }

    private BattleStatsVO durableFighter() {
        BattleStatsVO stats = new BattleStatsVO();
        stats.setHealth(100000);
        stats.setAttack(10);
        return stats;
    }

    @Test
    void rejectsFloorsOutsideOpenRange() {
        assertThrows(BusinessException.class, () -> service.getFloorMonster(0));
        assertThrows(BusinessException.class, () -> service.getFloorMonster(67));
    }

    @Test
    void progressContinuesFromPreviousCap() {
        mockProgress(54);
        TowerProgressVO progress = service.getProgress();
        assertEquals(54, progress.getMaxFloor());
        assertEquals(66, progress.getMaxOpenFloor());
        assertFalse(progress.getCompleted());
        assertEquals(55, progress.getNextFloor());
        assertEquals(100, progress.getNextMonster().getRewardPoints());
    }

    @Test
    void progressOffersLastOpenFloor() {
        mockProgress(65);
        TowerProgressVO progress = service.getProgress();
        assertEquals(66, progress.getMaxOpenFloor());
        assertFalse(progress.getCompleted());
        assertEquals(66, progress.getNextFloor());
        assertEquals(66, progress.getNextMonster().getFloor());
    }

    @Test
    void completedProgressHasNoNextFloorIncludingLegacyHigherProgress() {
        for (int floor : new int[]{66, 67}) {
            mockProgress(floor);
            TowerProgressVO progress = service.getProgress();
            assertEquals(floor, progress.getMaxFloor());
            assertEquals(66, progress.getMaxOpenFloor());
            assertTrue(progress.getCompleted());
            assertNull(progress.getNextFloor());
            assertNull(progress.getNextMonster());
        }
    }

    @Test
    void completedTowerCannotBeChallengedOrConsumeAttempts() {
        for (int floor : new int[]{66, 67}) {
            mockProgress(floor);
            BusinessException exception = assertThrows(BusinessException.class, service::challenge);
            assertTrue(exception.getMessage().contains("66"));
        }
        verifyNoInteractions(redisTemplate, petService, pointsService, recordMapper);
        verify(progressMapper, never()).updateById(any());
    }

    private void mockProgress(int maxFloor) {
        User user = new User();
        user.setId(1L);
        when(userService.getLoginUser()).thenReturn(user);
        TowerClimbProgress progress = new TowerClimbProgress();
        progress.setUserId(1L);
        progress.setMaxFloor(maxFloor);
        when(progressMapper.selectOne(any())).thenReturn(progress);
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyWonAndPersistedChallengeTriggersBroadcast() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        FishPet pet = new FishPet(); pet.setLevel(1);
        when(petService.getOne(any())).thenReturn(pet);
        PetEquipStatsVO stats = new PetEquipStatsVO();
        stats.setTotalBaseHp(1000000); stats.setTotalBaseAttack(1000000); stats.setSpeed(100);
        when(petService.getPetEquipStats()).thenReturn(stats);
        when(progressMapper.updateById(any())).thenReturn(1);
        when(recordMapper.insert(any())).thenReturn(1);
        mockProgress(54);
        TowerClimbResultVO result = service.challenge();
        assertTrue(result.getWin());
        assertEquals(55, result.getMaxFloor());
        assertEquals(100, result.getRewardPoints());
        verify(pointsService).updateUsedPoints(1L, -100, "tower_climb", "55", "爬塔第 55 层奖励");
        verify(broadcasts).towerCleared(1L, 55);
        clearInvocations(broadcasts);

        stats.setTotalBaseHp(0); stats.setTotalBaseAttack(0); stats.setSpeed(0);
        mockProgress(55);
        assertFalse(service.challenge().getWin());
        verifyNoInteractions(broadcasts);

        stats.setTotalBaseHp(1000000); stats.setTotalBaseAttack(1000000); stats.setSpeed(100);
        when(recordMapper.insert(any())).thenReturn(0);
        assertThrows(BusinessException.class, service::challenge);
        verifyNoInteractions(broadcasts);
    }
}
