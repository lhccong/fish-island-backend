package com.cong.fishisland.service.impl.user;

import com.cong.fishisland.constant.DailyPointCounter;
import com.cong.fishisland.mapper.pet.PetTreasureMapper;
import com.cong.fishisland.mapper.user.UserPointsRecordMapper;
import com.cong.fishisland.model.dto.turntable.TurntableQueryRequest;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.pet.PetTreasureVO;
import com.cong.fishisland.model.vo.user.DailyPointTasksVO;
import com.cong.fishisland.model.vo.user.DailyPointTasksVO.Task;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.TowerClimbService;
import com.cong.fishisland.service.UserSignInService;
import com.cong.fishisland.service.pet.PetTreasureService;
import com.cong.fishisland.service.turntable.TurntableDrawRecordService;
import com.cong.fishisland.service.turntable.TurntableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.*;

@Service
@RequiredArgsConstructor
public class DailyPointTasksService {
    private final UserSignInService signIn;
    private final UserPointsRecordMapper records;
    private final PetTreasureMapper companions;
    private final PetTreasureService treasure;
    private final TowerClimbService tower;
    private final FarmLandService farm;
    private final TurntableDrawRecordService draws;
    private final TurntableService turntables;
    private final StringRedisTemplate redis;

    public DailyPointTasksVO snapshot(Long userId) {
        // Use the same business day as sign-in, chat, moments, tower and draws.
        ZonedDateTime now = ZonedDateTime.now();
        ZoneId zone = now.getZone();
        LocalDate day = now.toLocalDate();
        long serverNow = now.toInstant().toEpochMilli();
        long resetAt = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long refreshAt = resetAt;
        Map<String, Long> earned = new HashMap<>();
        for (UserPointsRecordMapper.DailyEarned item : records.dailyEarned(
                userId, Date.from(day.atStartOfDay(zone).toInstant()), new Date(resetAt))) {
            earned.put(item.getSourceType(), item.getEarned());
        }

        List<Task> tasks = new ArrayList<>();
        tasks.add(quota(SIGN_IN.getValue(), signIn.hasSignedOn(userId, day) ? 1 : 0, 1, earned));
        DailyPointCounter[] counters = DailyPointCounter.values();
        List<String> values = redis.opsForValue().multiGet(Arrays.stream(counters)
                .map(counter -> counter.key(userId, day)).collect(Collectors.toList()));
        for (int i = 0; i < counters.length; i++) {
            DailyPointCounter counter = counters[i];
            String value = values.get(i);
            tasks.add(quota(counter.name().toLowerCase(Locale.ROOT),
                    value == null ? 0 : Integer.parseInt(value), counter.limit, earned));
        }

        boolean hasCompanion = companions.companionCount(userId) > 0;
        if (hasCompanion) {
            PetTreasureVO dig = treasure.status(userId);
            tasks.add(treasureTask(dig, earned));
            refreshAt = Math.min(refreshAt, dig.getResetAt());
            if (dig.getActive() == null && dig.getNextAvailableAt() > dig.getServerNow()) {
                refreshAt = Math.min(refreshAt, dig.getNextAvailableAt());
            }
        } else {
            tasks.add(opportunity(PET_TREASURE.getValue(), false, "需先领养伙伴", earned));
        }

        boolean freeDrawUsed = draws.hasTodayDrawRecord(userId);
        boolean hasDrawPool = !turntables.listActiveTurntables(new TurntableQueryRequest()).isEmpty();
        if (hasCompanion && (freeDrawUsed || hasDrawPool)) {
            tasks.add(new Task("free_draw", freeDrawUsed ? "done" : "ready", freeDrawUsed ? 1 : 0, 1,
                    points(earned, PRIZE_CONVERT.getValue()), null));
        } else {
            tasks.add(opportunity("free_draw", false, hasCompanion ? "暂无开放奖池" : "需先领养伙伴", earned));
        }

        boolean openFloor = hasCompanion && tower.hasOpenFloor(userId);
        int towerLeft = openFloor ? tower.getRemainingDailyChallenges(userId) : 0;
        tasks.add(opportunity(TOWER_CLIMB.getValue(), towerLeft > 0,
                !hasCompanion ? "需先领养伙伴" : !openFloor ? "已通关开放楼层"
                        : towerLeft > 0 ? "剩余 " + towerLeft + " 次挑战" : "今日挑战次数已用完", earned));
        tasks.add(opportunity(FARM_HARVEST.getValue(), farm.hasHarvestableLand(userId), null, earned));

        for (PointsRecordSourceEnum source : new PointsRecordSourceEnum[]{
                FARM_STEAL, ITEM_DECOMPOSE, PET_DAILY, TOURNAMENT_RANK, RED_PACKET_GRAB,
                LUCKY_BAG_WIN, MOMENTS_REWARD, SIGN_IN_MAKEUP, REDEEM_CODE, SPONSOR
        }) {
            tasks.add(opportunity(source.getValue(), false, null, earned));
        }
        tasks.add(opportunity("BOSS_KILL", false, null, earned));
        return new DailyPointTasksVO(serverNow, refreshAt, tasks);
    }

    private Task treasureTask(PetTreasureVO dig, Map<String, Long> earned) {
        String id = PET_TREASURE.getValue();
        int goal = PetTreasureService.DAILY_LIMIT;
        if (dig.getActive() != null) {
            int settled = Math.max(0, dig.getUsed() - 1);
            return new Task(id, "ready", settled, goal, points(earned, id), "有一次挖宝待结算");
        }
        if (dig.getUsed() >= goal || dig.getNextAvailableAt() <= dig.getServerNow()) {
            return quota(id, dig.getUsed(), goal, earned);
        }
        boolean today = dig.getNextAvailableAt() < dig.getResetAt();
        String detail = today ? "冷却至 " + new Date(dig.getNextAvailableAt()).toInstant()
                .atZone(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("HH:mm"))
                : "下次刷新在明天";
        return new Task(id, today ? "waiting" : "conditional", dig.getUsed(), goal, points(earned, id), detail);
    }

    private Task quota(String id, int progress, int goal, Map<String, Long> earned) {
        return new Task(id, progress >= goal ? "done" : "ready",
                Math.min(goal, Math.max(0, progress)), goal, points(earned, id), null);
    }

    private Task opportunity(String id, boolean available, String detail, Map<String, Long> earned) {
        return new Task(id, available ? "ready" : "conditional", null, null, points(earned, id), detail);
    }

    private long points(Map<String, Long> earned, String id) {
        return earned.getOrDefault(id, 0L);
    }
}
