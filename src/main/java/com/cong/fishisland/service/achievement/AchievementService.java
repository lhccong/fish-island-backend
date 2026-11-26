package com.cong.fishisland.service.achievement;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.PetForgeProperties;
import com.cong.fishisland.mapper.AchievementMapper;
import com.cong.fishisland.mapper.AchievementMapper.Equipment;
import com.cong.fishisland.mapper.AchievementMapper.Reward;
import com.cong.fishisland.model.enums.pay.PayOrderTypeEnum;
import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.pet.PetCardPlayRules;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.cong.fishisland.service.achievement.AchievementRule.*;

@Service
public class AchievementService {
    private static final BigDecimal MAJOR_SPONSOR_MINIMUM = BigDecimal.valueOf(100);

    private final AchievementMapper mapper;
    private final UserPointsService points;
    private final PetForgeProperties forge;
    private final TransactionTemplate transactions;

    public AchievementService(AchievementMapper mapper, UserPointsService points, PetForgeProperties forge,
                              PlatformTransactionManager manager) {
        this.mapper = mapper;
        this.points = points;
        this.forge = forge;
        this.transactions = new TransactionTemplate(manager);
    }

    @Transactional(readOnly = true)
    public Summary list(long userId) {
        return load(userId, false);
    }

    public Summary claim(long userId) {
        AtomicReference<Summary> result = new AtomicReference<>();
        points.runWithUserPointsLocks(new Long[]{userId}, () -> result.set(transactions.execute(status -> {
            if (mapper.lockPoints(userId) == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户积分不存在");
            }
            return load(userId, true);
        })));
        return result.get();
    }

    private Summary load(long userId, boolean claim) {
        Map<String, Map<Integer, Integer>> claimed = new HashMap<>();
        for (Reward reward : mapper.rewards(userId)) {
            claimed.computeIfAbsent(reward.getAchievementId(), id -> new HashMap<>())
                    .put(reward.getSubjectId(), reward.getClaimedTiers());
        }

        Long opened = mapper.openedPacks(userId);
        Map<Integer, Integer> equipment = mapper.equipmentLevels(userId).stream()
                .collect(Collectors.toMap(Equipment::getSlot, Equipment::getLevel));
        List<Progress> equipped = new ArrayList<>();
        for (EquipSlotEnum slot : EquipSlotEnum.values()) {
            int id = slot.getValue();
            int level = equipment.getOrDefault(id, 0);
            equipped.add(new Progress(id, slot.getLabel(), level, forge.includeForgeStatsForSlot(id)));
        }
        List<Achievement> achievements = new ArrayList<>();
        achievements.add(group(userId, USER, single(mapper.userLevel(userId)), claimed, claim));
        achievements.add(group(userId, TURNTABLE, single(mapper.turntableDrawCount(userId)), claimed, claim));
        BigDecimal sponsorship = mapper.maxPaidSponsorship(userId, PayOrderTypeEnum.SPONSOR.getText());
        achievements.add(group(userId, SPONSOR, sponsorshipProgress(sponsorship, BigDecimal.ONE), claimed, claim));
        achievements.add(group(userId, MAJOR_SPONSOR, sponsorshipProgress(sponsorship, MAJOR_SPONSOR_MINIMUM), claimed, claim));
        achievements.add(group(userId, FARM, single(mapper.farmLevel(userId)), claimed, claim));
        achievements.add(group(userId, CARDS, single(PetCardPlayRules.level(opened == null ? 0 : opened)), claimed, claim));
        achievements.add(group(userId, PET, single(mapper.petLevel(userId)), claimed, claim));
        achievements.add(group(userId, EQUIPMENT, equipped, claimed, claim));
        int awarded = claim ? achievements.stream().flatMap(item -> item.tracks.stream())
                .mapToInt(Track::getAwardedPoints).sum() : 0;
        return new Summary(achievements, awarded);
    }

    private List<Progress> single(Integer level) {
        return Collections.singletonList(new Progress(0, null, level == null ? 0 : level, true));
    }

    private List<Progress> sponsorshipProgress(BigDecimal amount, BigDecimal minimum) {
        return single(amount != null && amount.compareTo(minimum) >= 0 ? 1 : 0);
    }

    private int claimed(Map<String, Map<Integer, Integer>> rewards, AchievementRule rule, int subject) {
        return rewards.getOrDefault(rule.id, Collections.emptyMap()).getOrDefault(subject, 0);
    }

    private Achievement group(long userId, AchievementRule rule, List<Progress> progress,
                              Map<String, Map<Integer, Integer>> rewards, boolean claim) {
        List<Track> tracks = new ArrayList<>();
        for (Progress entry : progress) {
            int paidTiers = claimed(rewards, rule, entry.id);
            int currentTiers = rule.tiersAt(entry.level);
            int unclaimed = Math.max(currentTiers - paidTiers, 0) * rule.points;
            int awarded = 0;
            if (claim && unclaimed > 0) {
                int first = rule.milestoneAt(paidTiers + 1);
                int last = rule.milestoneAt(currentTiers);
                String milestones = (first == last ? String.valueOf(first) : first + "–" + last) + " " + rule.unit;
                mapper.saveReward(userId, rule.id, entry.id, currentTiers);
                points.updateUsedPoints(userId, -unclaimed, PointsRecordSourceEnum.ACHIEVEMENT.getValue(),
                        rule.id + ":" + entry.id + ":" + currentTiers,
                        rule.name + (entry.label == null ? "" : " · " + entry.label) + " · " + milestones);
                paidTiers = currentTiers;
                awarded = unclaimed;
                unclaimed = 0;
            }
            int reachedTier = Math.max(currentTiers, paidTiers);
            Integer next = reachedTier >= rule.tiersAt(rule.maxMilestone) ? null : rule.milestoneAt(reachedTier + 1);
            tracks.add(new Track(entry.id, entry.label, entry.level, entry.enabled, next, paidTiers, unclaimed, awarded));
        }
        return new Achievement(rule.id, rule.name, rule.label, rule.icon, rule.unit, rule.interval, rule.points, tracks);
    }

    @Value
    private static class Progress {
        int id;
        String label;
        int level;
        boolean enabled;
    }

    @Value
    public static class Track {
        int id;
        String label;
        int level;
        boolean enabled;
        Integer next;
        int claimedTiers;
        int claimablePoints;
        int awardedPoints;
    }

    @Value
    public static class Achievement {
        String id;
        String name;
        String label;
        String icon;
        String unit;
        int interval;
        int points;
        List<Track> tracks;
    }

    @Value
    public static class Summary {
        List<Achievement> achievements;
        int awardedPoints;
    }
}
