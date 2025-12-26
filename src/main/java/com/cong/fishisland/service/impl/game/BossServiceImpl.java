package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.AttackResultVO;
import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.BossBattleInfoVO;
import com.cong.fishisland.model.vo.game.BossChallengeRankingVO;
import com.cong.fishisland.model.vo.game.BossVO;
import com.cong.fishisland.service.BossService;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Boss服务实现类
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BossServiceImpl implements BossService {

    private final UserService userService;
    private final FishPetService fishPetService;
    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();

    // Boss血量缓存过期时间（24小时）
    private static final long BOSS_HEALTH_CACHE_EXPIRE_HOURS = 24;

    // 暴击概率（20%）
    private static final double CRITICAL_RATE = 0.2;
    // 闪避概率（15%）
    private static final double DODGE_RATE = 0.15;
    // 连击概率（10%）
    private static final double COMBO_RATE = 0.1;
    // 暴击伤害倍数
    private static final double CRITICAL_DAMAGE_MULTIPLIER = 2.0;
    // 连击伤害倍数
    private static final double COMBO_DAMAGE_MULTIPLIER = 1.5;

    @Override
    public List<BossVO> getBossList() {
        List<BossVO> bossList = new ArrayList<>();

        // Boss 1: 邪恶总监 (800积分)
        BossVO boss1 = new BossVO();
        boss1.setId(1L);
        boss1.setName("邪恶总监");
        boss1.setAvatar("https://img0.baidu.com/it/u=3023752464,2240102315&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500");
        boss1.setHealth(8000);
        boss1.setRewardPoints(800);
        boss1.setAttack(400);
        bossList.add(boss1);

        // Boss 2: 996领导 (900积分)
        BossVO boss2 = new BossVO();
        boss2.setId(2L);
        boss2.setName("996领导");
        boss2.setAvatar("https://img2.baidu.com/it/u=4291858461,3385772735&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=503");
        boss2.setHealth(9000);
        boss2.setRewardPoints(900);
        boss2.setAttack(450);
        bossList.add(boss2);

        // Boss 3: 集齐迈巴赫碎片老板 (1000积分)
        BossVO boss3 = new BossVO();
        boss3.setId(3L);
        boss3.setName("集齐迈巴赫碎片老板");
        boss3.setAvatar("https://img2.baidu.com/it/u=285303210,4203227947&fm=253&fmt=auto&app=138&f=JPEG?w=800&h=800");
        boss3.setHealth(10000);
        boss3.setRewardPoints(1000);
        boss3.setAttack(500);
        bossList.add(boss3);

        return bossList;
    }

    @Override
    public List<BattleResultVO> battle(Long bossId) {
        // 参数校验
        if (bossId == null || bossId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Boss ID不能为空");
        }

        // 获取当前登录用户
        Long userId = userService.getLoginUser().getId();

        // 检查用户今天是否已经打过这个Boss
        if (hasUserBattledToday(userId, bossId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您今天已经挑战过这个Boss了，请明天再来");
        }

        // 获取用户的宠物
        QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        FishPet pet = fishPetService.getOne(queryWrapper);
        if (pet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "您还没有宠物");
        }

        // 获取Boss信息
        BossVO boss = getBossById(bossId);
        if (boss == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Boss不存在");
        }

        // 计算宠物的攻击力和血量
        int petLevel = pet.getLevel() != null ? pet.getLevel() : 1;
        int petAttack = petLevel;  // 攻击力等于等级
        int petHealth = petLevel * 100;

        // 初始化血量（宠物每次对战都是满血开始）
        int currentPetHealth = petHealth;
        
        // 从Redis获取Boss当前血量，如果不存在则使用初始血量
        int currentBossHealth = getBossHealthFromRedis(bossId, boss.getHealth());

        // 创建对战结果列表
        List<BattleResultVO> battleResults = new ArrayList<>();
        
        // 初始化攻击顺序：第一回合宠物先攻击
        boolean petTurn = true;
        
        // 累计用户造成的总伤害
        int totalDamage = 0;
        
        // 执行10个回合的对战
        for (int round = 1; round <= 10; round++) {
            // 如果宠物或Boss血量已为0，提前结束
            if (currentPetHealth <= 0 || currentBossHealth <= 0) {
                break;
            }

            // 创建当前回合的对战结果对象
            BattleResultVO result = new BattleResultVO();

            // 根据当前回合决定攻击者（轮流攻击，除非连击）
            String attackerType = petTurn ? "PET" : "BOSS";
            result.setAttackerType(attackerType);

            // 执行一次攻击
            int damage = 0;
            boolean isDodge = false;
            boolean isCritical = false;
            boolean isCombo = false;

            if (petTurn) {
                // 宠物攻击Boss
                AttackResultVO attackResult = performAttack(petAttack, currentBossHealth);
                damage = attackResult.getDamage();
                isDodge = attackResult.isDodge();
                isCritical = attackResult.isCritical();
                isCombo = attackResult.isCombo();
                currentBossHealth = Math.max(0, currentBossHealth - damage);
                // 累计用户造成的伤害（只有非闪避的攻击才计入）
                if (!isDodge) {
                    totalDamage += damage;
                }
            } else {
                // Boss攻击宠物
                AttackResultVO attackResult = performAttack(boss.getAttack(), currentPetHealth);
                damage = attackResult.getDamage();
                isDodge = attackResult.isDodge();
                isCritical = attackResult.isCritical();
                isCombo = attackResult.isCombo();
                currentPetHealth = Math.max(0, currentPetHealth - damage);
            }

            // 设置攻击结果
            result.setDamage(damage);
            result.setIsDodge(isDodge);
            result.setIsCritical(isCritical);
            result.setIsCombo(isCombo);
            result.setIsNormalAttack(!isDodge && !isCritical && !isCombo);
            result.setPetRemainingHealth(currentPetHealth);
            result.setBossRemainingHealth(currentBossHealth);

            // 添加到结果列表
            battleResults.add(result);

            // 只有当Boss被攻击时，才更新Redis中的Boss血量
            if (petTurn) {
                updateBossHealthToRedis(bossId, currentBossHealth);
            }
            
            // 如果没有连击，切换到另一方；如果有连击，保持当前攻击者不变
            if (!isCombo) {
                petTurn = !petTurn;
            }
        }

        // 标记用户今天已挑战过这个Boss
        markUserBattledToday(userId, bossId);

        // 将用户造成的伤害存入排行榜
        updateBossChallengeRanking(bossId, userId, totalDamage);

        return battleResults;
    }

    /**
     * 执行攻击
     *
     * @param attackPower 攻击力
     * @param targetHealth 目标当前血量
     * @return 攻击结果
     */
    private AttackResultVO performAttack(int attackPower, int targetHealth) {
        AttackResultVO result = new AttackResultVO();

        // 判断是否闪避
        if (random.nextDouble() < DODGE_RATE) {
            result.setDodge(true);
            result.setDamage(0);
            return result;
        }

        // 判断是否连击
        boolean isCombo = random.nextDouble() < COMBO_RATE;
        result.setCombo(isCombo);

        // 判断是否暴击
        boolean isCritical = random.nextDouble() < CRITICAL_RATE;
        result.setCritical(isCritical);

        // 计算伤害
        double damageMultiplier = 1.0;
        if (isCritical) {
            damageMultiplier *= CRITICAL_DAMAGE_MULTIPLIER;
        }
        if (isCombo) {
            damageMultiplier *= COMBO_DAMAGE_MULTIPLIER;
        }

        // 伤害有10%的浮动
        double damageVariation = 0.9 + random.nextDouble() * 0.2; // 0.9-1.1
        int damage = (int) (attackPower * damageMultiplier * damageVariation);

        // 确保伤害不超过目标当前血量
        damage = Math.min(damage, targetHealth);

        result.setDamage(damage);
        return result;
    }

    /**
     * 根据ID获取Boss
     *
     * @param bossId Boss ID
     * @return Boss信息
     */
    private BossVO getBossById(Long bossId) {
        List<BossVO> bossList = getBossList();
        return bossList.stream()
                .filter(boss -> boss.getId().equals(bossId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从Redis获取Boss当前血量
     *
     * @param bossId Boss ID
     * @param defaultHealth 默认血量（如果Redis中没有数据）
     * @return Boss当前血量
     */
    private int getBossHealthFromRedis(Long bossId, Integer defaultHealth) {
        try {
            String healthKey = RedisKey.getKey(RedisKey.BOSS_HEALTH_CACHE_KEY, bossId);
            String healthStr = redisTemplate.opsForValue().get(healthKey);
            
            if (healthStr != null && !healthStr.isEmpty()) {
                int health = Integer.parseInt(healthStr);
                // 如果血量小于等于0，说明Boss已被击败，返回默认血量（重置）
                if (health <= 0) {
                    return defaultHealth;
                }
                return health;
            }
            
            // Redis中没有数据，返回默认血量并初始化到Redis
            updateBossHealthToRedis(bossId, defaultHealth);
            return defaultHealth;
        } catch (Exception e) {
            log.error("从Redis获取Boss血量失败，bossId: {}, 使用默认血量", bossId, e);
            return defaultHealth;
        }
    }

    /**
     * 更新Boss血量到Redis
     *
     * @param bossId Boss ID
     * @param health Boss当前血量
     */
    private void updateBossHealthToRedis(Long bossId, Integer health) {
        try {
            String healthKey = RedisKey.getKey(RedisKey.BOSS_HEALTH_CACHE_KEY, bossId);
            redisTemplate.opsForValue().set(
                    healthKey,
                    String.valueOf(health),
                    BOSS_HEALTH_CACHE_EXPIRE_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.error("更新Boss血量到Redis失败，bossId: {}, health: {}", bossId, health, e);
        }
    }

    /**
     * 检查用户今天是否已经挑战过这个Boss
     *
     * @param userId 用户ID
     * @param bossId Boss ID
     * @return 是否已挑战过
     */
    private boolean hasUserBattledToday(Long userId, Long bossId) {
        try {
            String dateStr = LocalDate.now().toString();
            String battleKey = RedisKey.getKey(RedisKey.BOSS_BATTLE_USER_DAILY_KEY, userId, bossId, dateStr);
            String value = redisTemplate.opsForValue().get(battleKey);
            return value != null && !value.isEmpty();
        } catch (Exception e) {
            log.error("检查用户Boss挑战记录失败，userId: {}, bossId: {}", userId, bossId, e);
            // 如果检查失败，为了安全起见，返回true（不允许挑战）
            return true;
        }
    }

    /**
     * 标记用户今天已挑战过这个Boss
     *
     * @param userId 用户ID
     * @param bossId Boss ID
     */
    private void markUserBattledToday(Long userId, Long bossId) {
        try {
            String dateStr = LocalDate.now().toString();
            String battleKey = RedisKey.getKey(RedisKey.BOSS_BATTLE_USER_DAILY_KEY, userId, bossId, dateStr);
            
            // 计算到明天凌晨的过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextDayMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            long expireSeconds = ChronoUnit.SECONDS.between(now, nextDayMidnight);
            
            redisTemplate.opsForValue().set(battleKey, "1", expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("标记用户Boss挑战记录失败，userId: {}, bossId: {}", userId, bossId, e);
        }
    }

    /**
     * 更新Boss挑战排行榜
     *
     * @param bossId Boss ID
     * @param userId 用户ID
     * @param damage 造成的伤害
     */
    private void updateBossChallengeRanking(Long bossId, Long userId, Integer damage) {
        try {
            String rankingKey = RedisKey.getKey(RedisKey.BOSS_CHALLENGE_RANKING_KEY, bossId);
            // 使用incrementScore累加伤害，如果用户不存在则自动创建
            redisTemplate.opsForZSet().incrementScore(rankingKey, userId.toString(), damage);
            // 设置过期时间为24小时
            redisTemplate.expire(rankingKey, BOSS_HEALTH_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("更新Boss挑战排行榜失败，bossId: {}, userId: {}, damage: {}", bossId, userId, damage, e);
        }
    }

    @Override
    public List<BossChallengeRankingVO> getBossChallengeRanking(Long bossId, Integer limit) {
        // 参数校验
        if (bossId == null || bossId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Boss ID不能为空");
        }
        
        // 设置默认限制
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        // 限制最大返回数量
        limit = Math.min(limit, 100);
        
        try {
            String rankingKey = RedisKey.getKey(RedisKey.BOSS_CHALLENGE_RANKING_KEY, bossId);
            // 获取分数最高的前limit条数据（按分数降序）
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, 0, limit - 1);
            
            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 使用原子计数器维护排名
            AtomicInteger rankCounter = new AtomicInteger(1);
            
            // 转换数据结构
            List<BossChallengeRankingVO> ranking = tuples.stream().map(tuple -> {
                BossChallengeRankingVO vo = new BossChallengeRankingVO();
                
                // 获取用户ID和伤害值
                Long userId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
                Integer damage = Objects.requireNonNull(tuple.getScore()).intValue();
                
                vo.setUserId(userId);
                vo.setDamage(damage);
                vo.setRank(rankCounter.getAndIncrement());
                
                // 获取用户信息
                try {
                    User user = userService.getById(userId);
                    if (user != null) {
                        vo.setUserName(user.getUserName());
                        vo.setUserAvatar(user.getUserAvatar());
                    }
                } catch (Exception e) {
                    log.error("获取用户信息失败，userId: {}", userId, e);
                }
                
                // 获取宠物信息
                try {
                    QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", userId);
                    FishPet pet = fishPetService.getOne(queryWrapper);
                    if (pet != null) {
                        vo.setPetName(pet.getName());
                        vo.setPetAvatar(pet.getPetUrl());
                    }
                } catch (Exception e) {
                    log.error("获取宠物信息失败，userId: {}", userId, e);
                }
                
                return vo;
            }).collect(Collectors.toList());
            
            return ranking;
        } catch (Exception e) {
            log.error("获取Boss挑战排行榜失败，bossId: {}, limit: {}", bossId, limit, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<BossVO> getBossListWithCache() {
        // 获取所有Boss的基础信息
        List<BossVO> bossList = getBossList();
        
        // 从Redis获取每个Boss的当前血量并更新
        bossList.forEach(boss -> {
            Integer currentHealth = getBossHealthFromRedis(boss.getId(), boss.getHealth());
            boss.setHealth(currentHealth);
        });
        
        return bossList;
    }

    @Override
    public BossBattleInfoVO getBossBattleInfo(Long bossId) {
        // 参数校验
        if (bossId == null || bossId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Boss ID不能为空");
        }

        // 获取当前登录用户
        Long userId = userService.getLoginUser().getId();

        // 获取用户的宠物
        QueryWrapper<FishPet> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        FishPet pet = fishPetService.getOne(queryWrapper);
        if (pet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "您还没有宠物");
        }

        // 获取Boss信息
        BossVO boss = getBossById(bossId);
        if (boss == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Boss不存在");
        }

        // 计算宠物的攻击力和血量
        int petLevel = pet.getLevel() != null ? pet.getLevel() : 1;
        int petAttack = petLevel;  // 攻击力等于等级
        int petHealth = petLevel * 100;

        // 从Redis获取Boss当前血量，如果不存在则使用初始血量
        int currentBossHealth = getBossHealthFromRedis(bossId, boss.getHealth());

        // 构建返回对象
        BossBattleInfoVO result = new BossBattleInfoVO();

        // 设置宠物信息
        BossBattleInfoVO.PetInfo petInfo = new BossBattleInfoVO.PetInfo();
        petInfo.setPetId(pet.getPetId());
        petInfo.setName(pet.getName());
        petInfo.setAvatar(pet.getPetUrl());
        petInfo.setLevel(petLevel);
        petInfo.setAttack(petAttack);
        petInfo.setHealth(petHealth);
        result.setPetInfo(petInfo);

        // 设置Boss信息
        BossBattleInfoVO.BossInfo bossInfo = new BossBattleInfoVO.BossInfo();
        bossInfo.setId(boss.getId());
        bossInfo.setName(boss.getName());
        bossInfo.setAvatar(boss.getAvatar());
        bossInfo.setAttack(boss.getAttack());
        bossInfo.setCurrentHealth(currentBossHealth);
        bossInfo.setMaxHealth(boss.getHealth());
        bossInfo.setRewardPoints(boss.getRewardPoints());
        result.setBossInfo(bossInfo);

        return result;
    }

}

