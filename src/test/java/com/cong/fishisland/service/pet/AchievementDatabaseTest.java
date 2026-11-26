package com.cong.fishisland.service.pet;

import com.cong.fishisland.config.PetForgeProperties;
import com.cong.fishisland.mapper.AchievementMapper;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.achievement.AchievementService;
import com.cong.fishisland.service.achievement.AchievementService.Achievement;
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

import java.math.BigDecimal;
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
                "file:sql/20261123_achievements.sql", "file:sql/turntable.sql", "file:sql/xunhupay.sql",
                "classpath:achievements/schema.sql")) {
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

            for (int i = 0; i < 199; i++) {
                fixtures.addDraw(1, i % 2 + 1, false);
            }
            for (int i = 0; i < 99; i++) {
                fixtures.addDraw(2, 1, false);
            }
            fixtures.addDraw(2, 1, true);
            String sponsor = "赞助摸鱼岛";
            fixtures.addOrder(1, "qualified", sponsor, new BigDecimal("1.00"), "OD", false);
            fixtures.addOrder(2, "small-1", sponsor, new BigDecimal("0.99"), "OD", false);
            fixtures.addOrder(2, "small-2", sponsor, new BigDecimal("0.99"), "OD", false);
            fixtures.addOrder(2, "pending", sponsor, BigDecimal.TEN, "PENDING", false);
            fixtures.addOrder(2, "refunded", sponsor, BigDecimal.TEN, "CD", false);
            fixtures.addOrder(2, "deleted", sponsor, BigDecimal.TEN, "OD", true);
            fixtures.addOrder(2, "other", "其他订单", BigDecimal.TEN, "OD", false);
            fixtures.addOrder(3, "rollback", sponsor, new BigDecimal("100.01"), "OD", false);

            Summary preview = service.list(1);
            assertEquals(4600, claimable(preview));
            assertEquals(0, preview.getAwardedPoints());
            Track sponsorship = track(preview, "island-sponsor");
            assertNull(sponsorship.getNext());
            assertEquals(1000, sponsorship.getClaimablePoints());
            assertEquals(0, track(preview, "island-major-sponsor").getClaimablePoints());
            Track userLevel = track(preview, "user-level");
            assertEquals(4, userLevel.getLevel());
            assertEquals(5, userLevel.getNext().intValue());
            assertEquals(600, userLevel.getClaimablePoints());
            Track draws = track(preview, "turntable-draw-count");
            assertEquals(199, draws.getLevel());
            assertEquals(200, draws.getNext().intValue());
            assertEquals(500, draws.getClaimablePoints());
            Achievement equipment = achievement(preview, "equipment-level");
            assertEquals(6, equipment.getTracks().size());
            Track necklace = equipment.getTracks().get(4);
            assertFalse(necklace.isEnabled());
            assertEquals(200, necklace.getClaimablePoints());
            forge.setIncludeNecklaceWingsStats(true);
            assertTrue(achievement(service.list(1), "equipment-level").getTracks().get(5).isEnabled());
            assertEquals(0, fixtures.balance(1));
            Summary initial = service.list(2);
            assertEquals(0, claimable(initial));
            assertEquals(1, track(initial, "island-sponsor").getNext().intValue());
            assertEquals(2, track(initial, "user-level").getNext().intValue());
            assertEquals(99, track(initial, "turntable-draw-count").getLevel());
            assertEquals(100, track(initial, "turntable-draw-count").getNext().intValue());
            Track introductoryCards = track(initial, "card-level");
            assertEquals(1, introductoryCards.getLevel());
            assertEquals(2, introductoryCards.getNext().intValue());

            ExecutorService workers = Executors.newFixedThreadPool(2);
            try {
                Future<Summary> first = workers.submit(() -> service.claim(1));
                Future<Summary> second = workers.submit(() -> service.claim(1));
                int firstReward = first.get(10, TimeUnit.SECONDS).getAwardedPoints();
                int secondReward = second.get(10, TimeUnit.SECONDS).getAwardedPoints();
                assertTrue(firstReward == 0 || secondReward == 0);
                assertEquals(4600, firstReward + secondReward);
            } finally {
                workers.shutdownNow();
            }
            assertEquals(4600, fixtures.balance(1));
            assertEquals(10, fixtures.entries(1));
            assertEquals(1, track(service.list(1), "island-sponsor").getClaimedTiers());
            fixtures.addOrder(1, "below-major", sponsor, new BigDecimal("99.99"), "OD", false);
            assertEquals(0, service.claim(1).getAwardedPoints());
            fixtures.advanceFarm(1, 30);
            assertEquals(200, service.claim(1).getAwardedPoints());
            assertEquals(4800, fixtures.balance(1));
            assertEquals(0, fixtures.balance(2));

            fixtures.addOrder(1, "major", sponsor, new BigDecimal("100.00"), "OD", false);
            Summary majorReward = service.claim(1);
            assertEquals(2000, majorReward.getAwardedPoints());
            assertEquals(1, track(majorReward, "island-major-sponsor").getClaimedTiers());
            fixtures.addOrder(1, "major-again", sponsor, new BigDecimal("101.00"), "OD", false);
            assertEquals(0, service.claim(1).getAwardedPoints());

            fixtures.advanceUser(2, 2);
            fixtures.addDraw(2, 2, false);
            Summary firstMilestones = service.claim(2);
            assertEquals(700, firstMilestones.getAwardedPoints());
            assertEquals(1, track(firstMilestones, "user-level").getClaimedTiers());
            assertEquals(1, track(firstMilestones, "turntable-draw-count").getClaimedTiers());

            fixtures.addDraw(1, 1, false);
            assertEquals(500, service.claim(1).getAwardedPoints());
            for (int i = 0; i < 100; i++) {
                fixtures.addDraw(1, 2, false);
            }
            Summary finalDrawMilestone = service.claim(1);
            assertEquals(500, finalDrawMilestone.getAwardedPoints());
            assertNull(track(finalDrawMilestone, "turntable-draw-count").getNext());
            for (int i = 0; i < 100; i++) {
                fixtures.addDraw(1, 1, false);
            }
            assertEquals(400, track(service.list(1), "turntable-draw-count").getLevel());
            assertEquals(0, service.claim(1).getAwardedPoints());

            assertThrows(IllegalStateException.class, () -> service.claim(3));
            assertTrue(mapper.rewards(3).isEmpty());
            assertEquals(0, fixtures.balance(3));
            assertEquals(0, fixtures.entries(3));
            failPetLedger.set(false);
            assertEquals(3700, service.claim(3).getAwardedPoints());
        }
    }

    private int claimable(Summary summary) {
        return summary.getAchievements().stream().flatMap(item -> item.getTracks().stream())
                .mapToInt(Track::getClaimablePoints).sum();
    }

    private Track track(Summary summary, String achievementId) {
        return achievement(summary, achievementId).getTracks().get(0);
    }

    private Achievement achievement(Summary summary, String achievementId) {
        return summary.getAchievements().stream().filter(item -> item.getId().equals(achievementId))
                .findFirst().orElseThrow(() -> new AssertionError("Missing achievement " + achievementId));
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

        @Update("UPDATE user_points SET level = #{level} WHERE userId = #{id}")
        void advanceUser(@Param("id") long id, @Param("level") int level);

        @Insert("INSERT INTO turntable_draw_record (userId, turntableId, turntablePrizeId, isDelete) "
                + "VALUES (#{id}, #{turntable}, 1, #{deleted})")
        void addDraw(@Param("id") long id, @Param("turntable") long turntable, @Param("deleted") boolean deleted);

        @Insert("INSERT INTO pay_order (userId, tradeOrderId, title, totalFee, status, isDelete) "
                + "VALUES (#{id}, #{order}, #{title}, #{fee}, #{status}, #{deleted})")
        void addOrder(@Param("id") long id, @Param("order") String order, @Param("title") String title,
                      @Param("fee") BigDecimal fee, @Param("status") String status, @Param("deleted") boolean deleted);
    }
}
