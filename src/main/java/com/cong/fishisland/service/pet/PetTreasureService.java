package com.cong.fishisland.service.pet;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.PetTreasureMapper;
import com.cong.fishisland.model.entity.pet.PetTreasureDig;
import com.cong.fishisland.model.vo.pet.PetTreasureVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.broadcast.AchievementBroadcastService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.PET_TREASURE;

@Service
public class PetTreasureService {
    public static final long DURATION = 5000;
    public static final long REFRESH = 3600000;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final PetTreasureMapper mapper;
    private final UserPointsService points;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final AchievementBroadcastService broadcasts;

    @Autowired
    public PetTreasureService(PetTreasureMapper mapper, UserPointsService points,
                              PlatformTransactionManager manager, AchievementBroadcastService broadcasts) {
        this(mapper, points, new TransactionTemplate(manager), Clock.systemUTC(), broadcasts);
    }

    // Clock injection keeps five-second, hourly and midnight boundaries testable.
    PetTreasureService(PetTreasureMapper mapper, UserPointsService points,
                       TransactionTemplate transactions, Clock clock, AchievementBroadcastService broadcasts) {
        this.mapper = mapper;
        this.points = points;
        this.transactions = transactions;
        this.clock = clock;
        this.broadcasts = broadcasts;
    }

    public PetTreasureVO status(Long userId) {
        return snapshot(userId, clock.millis());
    }

    public PetTreasureVO start(Long userId, String siteId) {
        if (!Arrays.asList("grove", "brook", "path", "flowers", "clearing").contains(siteId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择营地内的挖宝地点");
        }
        return locked(userId, () -> {
            long now = clock.millis();
            if (mapper.companionCount(userId) == 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先领养伙伴");
            }
            PetTreasureVO state = snapshot(userId, now);
            // A lost response or a second tab resumes the same operation.
            if (state.getActive() != null) return state;
            if (state.getUsed() >= 5) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "今天已挖宝五次，明天再来吧");
            }
            if (state.getNextAvailableAt() > now) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "宝藏尚未刷新，请稍后再来");
            }
            PetTreasureDig dig = new PetTreasureDig();
            dig.setId(UUID.randomUUID().toString());
            dig.setUserId(userId);
            dig.setDigDay(day(now));
            dig.setSlot(state.getUsed() + 1);
            dig.setSiteId(siteId);
            dig.setReadyAt(now + DURATION);
            dig.setReward(ThreadLocalRandom.current().nextInt(1, 11));
            if (mapper.insert(dig) != 1) throw failure();
            return snapshot(userId, now);
        });
    }

    public PetTreasureVO finish(Long userId, String id) {
        if (id == null || id.length() > 36) throw failure();
        return locked(userId, () -> {
            long now = clock.millis();
            PetTreasureDig dig = mapper.owned(userId, id);
            if (dig == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "挖宝记录不存在");
            if (dig.getCompletedAt() != null) return snapshot(userId, now);
            if (now < dig.getReadyAt()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "还在挖掘中，请稍候");
            }
            // Conditional write protects settlement even if a distributed lock lease expires.
            if (mapper.complete(userId, id, now) == 1) {
                points.updateUsedPoints(userId, -dig.getReward(), PET_TREASURE.getValue(), id, "营地挖宝奖励");
                broadcasts.treasureFound(userId, dig.getReward());
            }
            return snapshot(userId, now);
        });
    }

    private PetTreasureVO locked(Long userId, Supplier<PetTreasureVO> action) {
        AtomicReference<PetTreasureVO> result = new AtomicReference<>();
        // Hold the established account points lock through the database commit.
        points.runWithUserPointsLocks(new Long[]{userId}, () ->
                result.set(transactions.execute(transaction -> action.get())));
        if (result.get() == null) throw failure();
        return result.get();
    }

    private PetTreasureVO snapshot(Long userId, long now) {
        PetTreasureDig last = mapper.latest(userId);
        PetTreasureVO state = new PetTreasureVO();
        state.setServerNow(now);
        state.setResetAt(day(now).plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli());
        state.setUsed(mapper.countDay(userId, day(now)));
        long next = last == null ? 0 : last.getReadyAt() + REFRESH;
        state.setNextAvailableAt(state.getUsed() >= 5 ? Math.max(next, state.getResetAt()) : next);
        if (last != null && last.getCompletedAt() == null) {
            PetTreasureVO.Active active = new PetTreasureVO.Active();
            active.setId(last.getId());
            active.setSiteId(last.getSiteId());
            active.setReadyAt(last.getReadyAt());
            state.setActive(active);
        } else if (last != null) {
            PetTreasureVO.Reward reward = new PetTreasureVO.Reward();
            reward.setId(last.getId());
            reward.setSiteId(last.getSiteId());
            reward.setPoints(last.getReward());
            state.setLastReward(reward);
        }
        return state;
    }

    private LocalDate day(long now) {
        return Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate();
    }

    private BusinessException failure() {
        return new BusinessException(ErrorCode.OPERATION_ERROR, "挖宝操作未完成，请刷新后重试");
    }
}
