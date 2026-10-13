package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.TournamentProperties;
import com.cong.fishisland.constant.TournamentRedisKey;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.service.impl.game.PetTournamentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PetTournamentServiceTest {
    private final Map<String, Double> ranks = new HashMap<>();
    private final Map<String, String> slots = new HashMap<>();
    private final Map<String, String> counts = new HashMap<>();
    private PetTournamentServiceImpl service;
    private PetBattleService battle;
    private RLock lock;
    private ValueOperations<String, String> values;
    private String countKey;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        HashOperations<String, Object, Object> hash = mock(HashOperations.class);
        values = mock(ValueOperations.class);
        when(redis.opsForZSet()).thenReturn(zset);
        when(redis.opsForHash()).thenReturn(hash);
        when(redis.opsForValue()).thenReturn(values);
        when(zset.score(anyString(), anyString())).thenAnswer(i -> ranks.get(i.getArgument(1)));
        when(hash.get(anyString(), any())).thenAnswer(i -> slots.get(i.getArgument(1)));
        when(values.get(anyString())).thenAnswer(i -> counts.get(i.getArgument(0)));
        doAnswer(i -> {
            counts.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
        when(zset.rangeByScoreWithScores(anyString(), anyDouble(), anyDouble())).thenAnswer(i -> {
            double min = i.getArgument(1);
            double max = i.getArgument(2);
            Set<ZSetOperations.TypedTuple<String>> result = new LinkedHashSet<>();
            ranks.entrySet().stream().filter(e -> e.getValue() >= min && e.getValue() <= max)
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(e -> result.add(new DefaultTypedTuple<>(e.getKey(), e.getValue())));
            return result;
        });
        when(zset.add(anyString(), anyString(), anyDouble())).thenAnswer(i -> {
            ranks.put(i.getArgument(1), i.getArgument(2));
            return true;
        });
        when(hash.delete(anyString(), any())).thenAnswer(i -> {
            slots.remove(i.getArgument(1));
            return 1L;
        });
        doAnswer(i -> {
            slots.put(i.getArgument(1), i.getArgument(2));
            return null;
        }).when(hash).put(anyString(), any(), any());

        UserService users = mock(UserService.class);
        User user = new User();
        user.setId(5L);
        when(users.getLoginUser()).thenReturn(user);
        FishPetService pets = mock(FishPetService.class);
        when(pets.getOne(any(Wrapper.class))).thenReturn(new FishPet());
        battle = mock(PetBattleService.class);
        RedissonClient redisson = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redisson.getLock(TournamentRedisKey.LEADERBOARD_LOCK)).thenReturn(lock);
        service = new PetTournamentServiceImpl(redis, users, pets, battle,
                mock(UserPointsService.class), new TournamentProperties(), redisson);
        countKey = TournamentRedisKey.dailyChallengeKey(5L,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        setBattleResult(true);
    }

    @Test
    void rankedWinnerInsertsAndShiftsOnlyUntilOldRank() {
        for (int rank = 1; rank <= 6; rank++) {
            add(rank, rank);
        }
        assertEquals(2, service.challenge(2).getMyRank());
        assertRank(1, 1);
        assertRank(5, 2);
        assertRank(2, 3);
        assertRank(3, 4);
        assertRank(4, 5);
        assertRank(6, 6);
        assertEquals(6, ranks.size());
        assertEquals(6, slots.size());
        assertEquals("1", counts.get(countKey));
    }

    @Test
    void unrankedWinnerKeepsOpponentAndShiftsEntireTail() {
        add(1, 1);
        add(2, 2);
        add(3, 3);
        service.challenge(1);
        assertRank(5, 1);
        assertRank(1, 2);
        assertRank(2, 3);
        assertRank(3, 4);
        assertEquals(4, slots.size());
    }

    @Test
    void tenthChallengeAllowedAndEleventhRejectedEvenAfterLoss() {
        add(1, 1);
        add(5, 2);
        setBattleResult(false);
        counts.put(countKey, "9");
        assertFalse(service.challenge(1).getIsWin());
        assertEquals("10", counts.get(countKey));
        assertThrows(BusinessException.class, () -> service.challenge(1));
        verify(battle, times(1)).battle(1L);
        verify(lock, times(2)).unlock();
        assertRank(1, 1);
        assertRank(5, 2);
    }

    @Test
    void previousDayCountDoesNotBlockToday() {
        counts.put(TournamentRedisKey.dailyChallengeKey(5L,
                LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))), "10");
        assertTrue(service.challenge(1).getIsWin());
        assertEquals("1", counts.get(countKey));
        verify(values).set(eq(countKey), eq("1"), longThat(ttl -> ttl > 0 && ttl <= 86400),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void invalidRankDoesNotConsumeChallenge() {
        add(5, 1);
        assertThrows(BusinessException.class, () -> service.challenge(2));
        assertThrows(BusinessException.class, () -> service.challenge(0));
        assertTrue(counts.isEmpty());
        verifyNoInteractions(battle);
    }

    @Test
    void battleErrorDoesNotConsumeChallengeAndReleasesLock() {
        add(1, 1);
        when(battle.battle(1L)).thenThrow(new IllegalStateException("battle failed"));
        assertThrows(IllegalStateException.class, () -> service.challenge(1));
        assertTrue(counts.isEmpty());
        assertRank(1, 1);
        verify(lock).unlock();
    }

    @Test
    void vacantSlotStillConsumesChallengeAndClearsOldSlot() {
        add(5, 3);
        assertTrue(service.challenge(1).getIsWin());
        assertRank(5, 1);
        assertFalse(slots.containsKey("3"));
        assertEquals("1", counts.get(countKey));
        verifyNoInteractions(battle);
    }

    private void add(long userId, int rank) {
        ranks.put(String.valueOf(userId), (double) rank);
        slots.put(String.valueOf(rank), String.valueOf(userId));
    }

    private void assertRank(long userId, int rank) {
        assertEquals(Double.valueOf(rank), ranks.get(String.valueOf(userId)));
        assertEquals(String.valueOf(userId), slots.get(String.valueOf(rank)));
    }

    private void setBattleResult(boolean win) {
        PetBattleResultVO round = new PetBattleResultVO();
        round.setMyPetRemainingHealth(win ? 100 : 0);
        round.setOpponentPetRemainingHealth(win ? 0 : 100);
        when(battle.battle(anyLong())).thenReturn(Collections.singletonList(round));
    }
}
