package com.cong.fishisland.service.pet;

import com.cong.fishisland.config.PetForgeProperties;
import com.cong.fishisland.mapper.AchievementMapper;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.achievement.AchievementService;
import com.cong.fishisland.service.achievement.AchievementService.Summary;
import com.cong.fishisland.service.achievement.AchievementService.Track;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "ACHIEVEMENT_VERIFY_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/fish_achievement_verify.*")
class AchievementDatabaseTest {
    @Test
    void usesSavedProgressAndPaysEachMilestoneOnceEvenWithConcurrentClaims() throws Exception {
        try (AnnotationConfigApplicationContext db = PetCardDatabaseContext.open(System.getenv("ACHIEVEMENT_VERIFY_JDBC"),
                "file:sql/20261122_pet_card_catalog.sql", "file:sql/20261123_pet_card_play.sql",
                "file:sql/20261123_achievements.sql", "classpath:achievements/schema.sql")) {
            SqlSessionTemplate sessions = db.getBean(SqlSessionTemplate.class);
            sessions.getConfiguration().addMapper(Fixtures.class);
            Fixtures fixtures = sessions.getMapper(Fixtures.class);
            AtomicBoolean failPetLedger = new AtomicBoolean(true);
            UserPointsService points = mock(UserPointsService.class);
            doAnswer(call -> {
                // Bypass the distributed lock to exercise the database row lock directly.
                call.getArgument(1, Runnable.class).run();
                return null;
            }).when(points).runWithUserPointsLocks(any(Long[].class), any(Runnable.class));
            doAnswer(call -> {
                long id = call.getArgument(0, Long.class);
                int delta = call.getArgument(1, Integer.class);
                String source = call.getArgument(3, String.class);
                fixtures.credit(id, delta);
                fixtures.record(id, source, -delta);
                if (id == 3 && source.startsWith("pet-level:") && failPetLedger.get()) {
                    throw new IllegalStateException("simulated ledger failure");
                }
                return null;
            }).when(points).updateUsedPoints(anyLong(), anyInt(), eq("achievement"), anyString(), anyString());
            AchievementMapper mapper = db.getBean(AchievementMapper.class);
            PetForgeProperties forge = new PetForgeProperties();
            AchievementService service = new AchievementService(mapper, points, forge,
                    db.getBean(PlatformTransactionManager.class));

            Summary preview = service.list(1);
            assertEquals(2500, claimable(preview));
            assertEquals(0, preview.getAwardedPoints());
            assertEquals(6, preview.getAchievements().get(2).getTracks().size());
            Track necklace = preview.getAchievements().get(2).getTracks().get(4);
            assertFalse(necklace.isEnabled());
            assertEquals(200, necklace.getClaimablePoints());
            forge.setIncludeNecklaceWingsStats(true);
            assertTrue(service.list(1).getAchievements().get(2).getTracks().get(5).isEnabled());
            assertEquals(0, fixtures.balance(1));
            assertEquals(0, claimable(service.list(2)));
            Track introductoryCards = service.list(2).getAchievements().get(1).getTracks().get(0);
            assertEquals(1, introductoryCards.getLevel());
            assertEquals(2, introductoryCards.getNext().intValue());

            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                Future<Summary> first = workers.submit(() -> service.claim(1));
                Future<Summary> second = workers.submit(() -> service.claim(1));
                int firstReward = first.get(10, TimeUnit.SECONDS).getAwardedPoints();
                int secondReward = second.get(10, TimeUnit.SECONDS).getAwardedPoints();
                assertTrue(firstReward == 0 || secondReward == 0);
                assertEquals(2500, firstReward + secondReward);
            } finally {
                workers.shutdownNow();
            }
            assertEquals(2500, fixtures.balance(1));
            assertEquals(7, fixtures.entries(1));
            assertEquals(0, service.claim(1).getAwardedPoints());
            fixtures.advanceFarm(1, 30);
            assertEquals(200, service.claim(1).getAwardedPoints());
            assertEquals(2700, fixtures.balance(1));
            assertEquals(0, fixtures.balance(2));

            assertThrows(IllegalStateException.class, () -> service.claim(3));
            assertTrue(mapper.rewards(3).isEmpty());
            assertEquals(0, fixtures.balance(3));
            assertEquals(0, fixtures.entries(3));
            failPetLedger.set(false);
            assertEquals(700, service.claim(3).getAwardedPoints());
        }
    }

    private int claimable(Summary summary) {
        return summary.getAchievements().stream().flatMap(item -> item.getTracks().stream())
                .mapToInt(Track::getClaimablePoints).sum();
    }

    interface Fixtures {
        @Update("UPDATE user_points SET usedPoints = usedPoints + #{delta} WHERE userId = #{id}")
        void credit(@Param("id") long id, @Param("delta") int delta);

        @Insert("INSERT INTO achievement_verify_ledger VALUES (#{id}, #{source}, #{points})")
        void record(@Param("id") long id, @Param("source") String source, @Param("points") int points);

        @Select("SELECT -usedPoints FROM user_points WHERE userId = #{id}")
        int balance(@Param("id") long id);

        @Select("SELECT COUNT(*) FROM achievement_verify_ledger WHERE user_id = #{id}")
        int entries(@Param("id") long id);

        @Update("UPDATE farm_user SET level = #{level} WHERE userId = #{id}")
        void advanceFarm(@Param("id") long id, @Param("level") int level);
    }
}
