package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.TournamentProperties;
import com.cong.fishisland.constant.TournamentRedisKey;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.model.vo.game.TournamentChallengeResultVO;
import com.cong.fishisland.model.vo.game.TournamentRankVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetBattleService;
import com.cong.fishisland.service.PetTournamentService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 宠物武道大会服务实现
 * <p>
 * Redis 结构：
 * - LEADERBOARD (ZSet)  member=userId, score=targetRank（名次越小score越小，ZRANGE正序即排名顺序）
 * - SLOTS (Hash)        field=rank, value=userId（快速查询某坑位的占位者）
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetTournamentServiceImpl implements PetTournamentService {

    /** 排行积分分配百分比（与 Boss 结算规则一致） */
    private static final double[] RANK_REWARD_PERCENTAGES = {0.20, 0.15, 0.10, 0.08, 0.08, 0.05, 0.05, 0.05, 0.05, 0.05};
    private static final double OTHER_PARTICIPANTS_REWARD_PERCENTAGE = 0.02;
    private static final DateTimeFormatter REWARD_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_DAILY_CHALLENGES = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final UserService userService;
    private final FishPetService fishPetService;
    private final PetBattleService petBattleService;
    private final UserPointsService userPointsService;
    private final TournamentProperties tournamentProperties;
    private final RedissonClient redissonClient;

    // ----------------------------------------------------------------
    // 公开接口
    // ----------------------------------------------------------------

    @Override
    public TournamentChallengeResultVO challenge(int targetRank) {
        RLock lock = redissonClient.getLock(TournamentRedisKey.LEADERBOARD_LOCK);
        lock.lock();
        try {
            return challengeUnderLock(targetRank);
        } finally {
            lock.unlock();
        }
    }

    private TournamentChallengeResultVO challengeUnderLock(int targetRank) {
        if (targetRank < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标名次不能小于1");
        }

        User me = userService.getLoginUser();
        long myUserId = me.getId();

        // 校验自己有宠物
        getPetByUserId(myUserId);

        // 获取我当前排名
        Integer myCurrentRank = getRankByUserId(myUserId);

        // 校验：有排名且排名 <= targetRank，不允许挑战（已经在更好的位置了）
        if (myCurrentRank != null && myCurrentRank <= targetRank) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "您当前排名第" + myCurrentRank + "，无需挑战第" + targetRank + "位");
        }

        // 查询目标坑位是否有人
        String slotOccupant = (String) stringRedisTemplate.opsForHash()
                .get(TournamentRedisKey.SLOTS, String.valueOf(targetRank));

        TournamentChallengeResultVO result = new TournamentChallengeResultVO();
        result.setTargetRank(targetRank);

        LocalDateTime now = LocalDateTime.now();
        String countKey = TournamentRedisKey.dailyChallengeKey(myUserId, now.format(REWARD_DATE_FORMAT));
        String countValue = stringRedisTemplate.opsForValue().get(countKey);
        int challengeCount = countValue == null ? 0 : Integer.parseInt(countValue);
        if (challengeCount >= MAX_DAILY_CHALLENGES) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "武道大会每天最多挑战10次，请明天再来");
        }

        if (slotOccupant == null) {
            // 坑位无人，直接占坑
            recordChallenge(countKey, challengeCount, now);
            occupySlot(myUserId, targetRank, myCurrentRank);
            result.setIsWin(true);
            result.setMyRank(targetRank);
            result.setOpponentUserId(null);
            result.setRounds(Collections.emptyList());
            log.info("用户{}直接占据武道大会第{}位（无人占坑）", myUserId, targetRank);
        } else {
            long opponentUserId = Long.parseLong(slotOccupant);

            if (opponentUserId == myUserId) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经占据该位置");
            }

            // 发起对战
            List<PetBattleResultVO> rounds = petBattleService.battle(opponentUserId);
            boolean isWin = determineWinner(rounds);
            recordChallenge(countKey, challengeCount, now);

            result.setIsWin(isWin);
            result.setOpponentUserId(opponentUserId);
            result.setRounds(rounds);

            if (isWin) {
                // 胜利：插入目标名次，受影响的参赛者依次后移
                occupySlot(myUserId, targetRank, myCurrentRank);
                result.setMyRank(targetRank);
                log.info("用户{}挑战第{}位成功，击败用户{}", myUserId, targetRank, opponentUserId);
            } else {
                result.setMyRank(myCurrentRank);
                log.info("用户{}挑战第{}位失败，被用户{}击败", myUserId, targetRank, opponentUserId);
            }
        }

        return result;
    }

    @Override
    public List<TournamentRankVO> getLeaderboard() {
        // 当前用户没排名则自动入榜（排到末位）
        autoJoinIfAbsent();

        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().rangeWithScores(TournamentRedisKey.LEADERBOARD, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有 userId
        List<Long> userIds = tuples.stream()
                .filter(t -> t.getValue() != null)
                .map(t -> Long.parseLong(t.getValue()))
                .collect(Collectors.toList());

        // 批量查用户（1 次）
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查宠物（1 次）
        Map<Long, FishPet> petMap = fishPetService.list(
                new QueryWrapper<FishPet>().in("userId", userIds)).stream()
                .collect(Collectors.toMap(FishPet::getUserId, p -> p));

        List<TournamentRankVO> list = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            long userId = Long.parseLong(tuple.getValue());
            int rank = tuple.getScore().intValue();
            TournamentRankVO vo = buildRankVO(userId, rank, userMap.get(userId), petMap.get(userId));
            if (vo != null) {
                list.add(vo);
            }
        }

        list.sort(Comparator.comparingInt(TournamentRankVO::getRank));
        return list;
    }

    @Override
    public Integer getMyRank() {
        // 校验有宠物才能参与
        long myUserId = userService.getLoginUser().getId();
        getPetByUserId(myUserId);
        return autoJoinIfAbsent();
    }

    @Override
    public void resetDailyLeaderboard() {
        log.info("开始重置武道大会排行榜");
        RLock lock = redissonClient.getLock(TournamentRedisKey.LEADERBOARD_LOCK);
        lock.lock();
        try {
            stringRedisTemplate.delete(Arrays.asList(TournamentRedisKey.LEADERBOARD, TournamentRedisKey.SLOTS));
        } finally {
            lock.unlock();
        }
        log.info("武道大会排行榜重置完成");
    }

    @Override
    public void distributeDailyRankRewards() {
        int totalRewardPoints = tournamentProperties.getRewardPoints();
        if (totalRewardPoints <= 0) {
            log.info("武道大会排行奖励积分为0，跳过发放");
            return;
        }

        try {
            String dateStr = LocalDate.now().format(REWARD_DATE_FORMAT);
            String rewardDistributedKey = TournamentRedisKey.rewardDistributedKey(dateStr);
            Boolean alreadyDistributed = stringRedisTemplate.opsForValue().setIfAbsent(
                    rewardDistributedKey, "1", 24, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(alreadyDistributed)) {
                log.info("武道大会排行奖励今日已发放，跳过");
                return;
            }

            Set<ZSetOperations.TypedTuple<String>> tuples =
                    stringRedisTemplate.opsForZSet().rangeWithScores(TournamentRedisKey.LEADERBOARD, 0, -1);
            if (tuples == null || tuples.isEmpty()) {
                log.warn("武道大会排行榜为空，跳过奖励发放");
                return;
            }

            log.info("开始发放武道大会排行奖励，总积分: {}，参与人数: {}", totalRewardPoints, tuples.size());

            int rankIndex = 0;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null) {
                    continue;
                }
                Long userId = Long.parseLong(tuple.getValue());
                double percentage = rankIndex < RANK_REWARD_PERCENTAGES.length
                        ? RANK_REWARD_PERCENTAGES[rankIndex]
                        : OTHER_PARTICIPANTS_REWARD_PERCENTAGE;
                int rewardPoints = (int) (totalRewardPoints * percentage);
                if (rewardPoints <= 0) {
                    rankIndex++;
                    continue;
                }

                int displayRank = rankIndex + 1;
                String description = rankIndex < RANK_REWARD_PERCENTAGES.length
                        ? "武道大会获得第" + displayRank + "名奖励"
                        : "武道大会获得参与奖励（第" + displayRank + "名）";
                try {
                    userPointsService.updateUsedPoints(userId, -rewardPoints,
                            PointsRecordSourceEnum.TOURNAMENT_RANK.getValue(),
                            dateStr, description);
                    log.info("武道大会排行奖励发放成功，userId: {}, rank: {}, rewardPoints: {}",
                            userId, displayRank, rewardPoints);
                } catch (Exception e) {
                    log.error("武道大会排行奖励发放失败，userId: {}, rank: {}", userId, displayRank, e);
                }
                rankIndex++;
            }

            log.info("武道大会排行奖励发放完成，共{}人获得奖励", rankIndex);
        } catch (Exception e) {
            log.error("武道大会排行奖励发放异常", e);
        }
    }

    // ----------------------------------------------------------------
    // 私有工具方法
    // ----------------------------------------------------------------

    /**
     * 插入目标名次，目标至旧名次之前的所有参赛者后移一位。
     * 未上榜的挑战者插入时，目标名次及之后的所有参赛者后移。
     * 调用方须持有排行榜锁。
     */
    private void occupySlot(long myUserId, int targetRank, Integer myOldRank) {
        String myUserIdStr = String.valueOf(myUserId);

        Set<ZSetOperations.TypedTuple<String>> affected = stringRedisTemplate.opsForZSet()
                .rangeByScoreWithScores(TournamentRedisKey.LEADERBOARD, targetRank,
                        myOldRank == null ? Double.POSITIVE_INFINITY : myOldRank - 1);
        if (myOldRank != null) {
            stringRedisTemplate.opsForHash().delete(TournamentRedisKey.SLOTS, String.valueOf(myOldRank));
        }
        if (affected != null) {
            // 先清理旧位置，再写入新位置，避免相邻名次相互覆盖。
            for (ZSetOperations.TypedTuple<String> tuple : affected) {
                stringRedisTemplate.opsForHash().delete(TournamentRedisKey.SLOTS,
                        String.valueOf(tuple.getScore().intValue()));
            }
            for (ZSetOperations.TypedTuple<String> tuple : affected) {
                int newRank = tuple.getScore().intValue() + 1;
                stringRedisTemplate.opsForZSet().add(TournamentRedisKey.LEADERBOARD, tuple.getValue(), newRank);
                stringRedisTemplate.opsForHash().put(TournamentRedisKey.SLOTS, String.valueOf(newRank), tuple.getValue());
            }
        }

        // 写入我的新坑位
        stringRedisTemplate.opsForZSet().add(TournamentRedisKey.LEADERBOARD, myUserIdStr, targetRank);
        stringRedisTemplate.opsForHash().put(TournamentRedisKey.SLOTS, String.valueOf(targetRank), myUserIdStr);
    }

    /**
     * 判断我方是否胜利：
     * 1. 对手血量归零 → 我方胜
     * 2. 回合耗尽（双方都有血）→ 我方剩余血量更多则胜
     */
    private boolean determineWinner(List<PetBattleResultVO> rounds) {
        if (rounds == null || rounds.isEmpty()) return false;
        PetBattleResultVO last = rounds.get(rounds.size() - 1);
        int myHp = last.getMyPetRemainingHealth();
        int opponentHp = last.getOpponentPetRemainingHealth();
        // 对手血量归零，或回合耗尽时我方血量更多
        return opponentHp <= 0 || (myHp > 0 && myHp > opponentHp);
    }

    /**
     * 如果当前用户没有排名，自动放到末位并返回新排名；已有排名则直接返回
     */
    private Integer autoJoinIfAbsent() {
        RLock lock = redissonClient.getLock(TournamentRedisKey.LEADERBOARD_LOCK);
        lock.lock();
        try {
            return autoJoinUnderLock();
        } finally {
            lock.unlock();
        }
    }

    private Integer autoJoinUnderLock() {
        long myUserId = userService.getLoginUser().getId();
        getPetByUserId(myUserId);

        Integer myRank = getRankByUserId(myUserId);
        if (myRank != null) {
            return myRank;
        }

        // 取当前最大 score（最末名次），新排名 = 最末 + 1；榜单为空则从第 1 位开始
        int lastRank = 0;
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> last =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(TournamentRedisKey.LEADERBOARD, 0, 0);
        if (last != null && !last.isEmpty()) {
            Double maxScore = last.iterator().next().getScore();
            if (maxScore != null) {
                lastRank = maxScore.intValue();
            }
        }
        int newRank = lastRank + 1;

        String myUserIdStr = String.valueOf(myUserId);
        stringRedisTemplate.opsForZSet().add(TournamentRedisKey.LEADERBOARD, myUserIdStr, newRank);
        stringRedisTemplate.opsForHash().put(TournamentRedisKey.SLOTS, String.valueOf(newRank), myUserIdStr);

        log.info("用户{}自动入榜，分配至第{}位", myUserId, newRank);
        return newRank;
    }

    /**
     * 获取用户当前排名（无排名返回null）
     */
    private Integer getRankByUserId(long userId) {
        Double score = stringRedisTemplate.opsForZSet()
                .score(TournamentRedisKey.LEADERBOARD, String.valueOf(userId));
        return score == null ? null : score.intValue();
    }

    private FishPet getPetByUserId(long userId) {
        QueryWrapper<FishPet> qw = new QueryWrapper<>();
        qw.eq("userId", userId);
        FishPet pet = fishPetService.getOne(qw);
        if (pet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "您还没有宠物，无法参加武道大会");
        }
        return pet;
    }

    private TournamentRankVO buildRankVO(long userId, int rank, User user, FishPet pet) {
        try {
            TournamentRankVO vo = new TournamentRankVO();
            vo.setRank(rank);
            vo.setUserId(userId);
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
            if (pet != null) {
                vo.setPetName(pet.getName());
                vo.setPetLevel(pet.getLevel());
                vo.setPetUrl(pet.getPetUrl());
            }
            return vo;
        } catch (Exception e) {
            log.warn("构建排行榜条目失败，userId={}", userId, e);
            return null;
        }
    }

    private void recordChallenge(String countKey, int challengeCount, LocalDateTime now) {
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long ttl = Math.max(1, java.time.Duration.between(LocalDateTime.now(), midnight).getSeconds());
        stringRedisTemplate.opsForValue().set(countKey, String.valueOf(challengeCount + 1), ttl, TimeUnit.SECONDS);
    }
}
