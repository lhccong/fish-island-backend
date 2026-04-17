package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.constant.TournamentRedisKey;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.model.vo.game.TournamentChallengeResultVO;
import com.cong.fishisland.model.vo.game.TournamentRankVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetBattleService;
import com.cong.fishisland.service.PetTournamentService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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

    private final StringRedisTemplate stringRedisTemplate;
    private final UserService userService;
    private final FishPetService fishPetService;
    private final PetBattleService petBattleService;

    /** 同一对手每日只能挑战一次的冷却时间（秒到当天结束） */
    // CD 时间动态计算，见 secondsUntilMidnight()

    // ----------------------------------------------------------------
    // 公开接口
    // ----------------------------------------------------------------

    @Override
    public TournamentChallengeResultVO challenge(int targetRank) {
        if (targetRank < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标名次不能小于1");
        }

        User me = userService.getLoginUser();
        long myUserId = me.getId();

        // 校验自己有宠物
        FishPet myPet = getPetByUserId(myUserId);

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

        if (slotOccupant == null) {
            // 坑位无人，直接占坑
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

            result.setIsWin(isWin);
            result.setOpponentUserId(opponentUserId);
            result.setRounds(rounds);

            if (isWin) {
                // 胜利：占据目标坑位，对手失去该坑位
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
        stringRedisTemplate.delete(TournamentRedisKey.LEADERBOARD);
        stringRedisTemplate.delete(TournamentRedisKey.SLOTS);
        log.info("武道大会排行榜重置完成");
    }

    // ----------------------------------------------------------------
    // 私有工具方法
    // ----------------------------------------------------------------

    /**
     * 占据坑位：
     * 1. 从 ZSet 中移除我的旧排名（如有）
     * 2. 将我写入 ZSet（score = targetRank）
     * 3. 更新 SLOTS Hash
     * 4. 如果目标坑位原来有人，将其从 ZSet 和 SLOTS 中移除
     */
    private void occupySlot(long myUserId, int targetRank, Integer myOldRank) {
        String myUserIdStr = String.valueOf(myUserId);

        // 查出目标坑位的原占位者
        String oldOccupant = (String) stringRedisTemplate.opsForHash()
                .get(TournamentRedisKey.SLOTS, String.valueOf(targetRank));

        // 先清目标坑位
        stringRedisTemplate.opsForHash().delete(TournamentRedisKey.SLOTS, String.valueOf(targetRank));
        if (oldOccupant != null && !oldOccupant.equals(myUserIdStr)) {
            stringRedisTemplate.opsForZSet().remove(TournamentRedisKey.LEADERBOARD, oldOccupant);
        }

        // 清我的旧坑位
        if (myOldRank != null) {
            stringRedisTemplate.opsForZSet().remove(TournamentRedisKey.LEADERBOARD, myUserIdStr);
            stringRedisTemplate.opsForHash().delete(TournamentRedisKey.SLOTS, String.valueOf(myOldRank));

            // 被打败的原占位者降到我的旧坑位
            if (oldOccupant != null && !oldOccupant.equals(myUserIdStr)) {
                stringRedisTemplate.opsForZSet().add(TournamentRedisKey.LEADERBOARD, oldOccupant, myOldRank);
                stringRedisTemplate.opsForHash().put(TournamentRedisKey.SLOTS, String.valueOf(myOldRank), oldOccupant);
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

                // 装备属性（直接传 pet，不再重复查库）
                PetEquipStatsVO equipStats = fishPetService.getPetEquipStatsByPet(pet);
                vo.setEquipStats(equipStats);

                // 实际战斗属性
                int lv = pet.getLevel() != null ? pet.getLevel() : 1;
                int equipAtk = equipStats != null && equipStats.getTotalBaseAttack() != null ? equipStats.getTotalBaseAttack() : 0;
                int equipHp = equipStats != null && equipStats.getTotalBaseHp() != null ? equipStats.getTotalBaseHp() : 0;
                vo.setAttack((int) (BattleConstant.BASE_ATK * Math.pow(1 + BattleConstant.GROWTH_RATE, lv)) + equipAtk);
                vo.setHealth(lv * 100 + equipHp);
            }
            return vo;
        } catch (Exception e) {
            log.warn("构建排行榜条目失败，userId={}", userId, e);
            return null;
        }
    }

    /**
     * 计算距离今天午夜的秒数
     */
    private static long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return java.time.Duration.between(now, midnight).getSeconds();
    }
}
