package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.RedisKey;
import com.cong.fishisland.mapper.game.BossMapper;
import com.cong.fishisland.model.entity.game.Boss;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.AttackResultVO;
import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.BossBattleInfoVO;
import com.cong.fishisland.model.vo.game.BossChallengeRankingVO;
import com.cong.fishisland.model.vo.game.BossVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.cong.fishisland.service.BossService;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.UserPointsService;
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
    private final UserPointsService userPointsService;
    private final StringRedisTemplate redisTemplate;
    private final BossMapper bossMapper;
    private final Random random = new Random();

    // Boss血量缓存过期时间（24小时）
    private static final long BOSS_HEALTH_CACHE_EXPIRE_HOURS = 24;

    // 每天最大挑战次数
    private static final int MAX_DAILY_CHALLENGES = 2;

    // 暴击伤害倍数
    private static final double CRITICAL_DAMAGE_MULTIPLIER = 2.0;
    // 连击伤害倍数
    private static final double COMBO_DAMAGE_MULTIPLIER = 1.5;

    // Boss击败积分分配百分比（按排名）
    // 第1名: 20%, 第2名: 15%, 第3名: 10%, 第4-5名: 各8%, 第6-10名: 各5%
    private static final double[] BOSS_REWARD_PERCENTAGES = {0.20, 0.15, 0.10, 0.08, 0.08, 0.05, 0.05, 0.05, 0.05, 0.05};
    
    // 其他参与者的奖励比例
    private static final double OTHER_PARTICIPANTS_REWARD_PERCENTAGE = 0.02;

    @Override
    public List<BossVO> getBossList() {
        // 从数据库查询所有启用的Boss
        QueryWrapper<Boss> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1).eq("isDelete", 0).orderByAsc("sort");
        List<Boss> bossList = bossMapper.selectList(queryWrapper);
        
        // 转换为BossVO
        return bossList.stream().map(boss -> {
            BossVO vo = new BossVO();
            vo.setId(boss.getId());
            vo.setName(boss.getName());
            vo.setAvatar(boss.getAvatar());
            vo.setHealth(boss.getHealth());
            vo.setAttack(boss.getAttack());
            vo.setRewardPoints(boss.getRewardPoints());
            // 主动属性
            vo.setCritRate(boss.getCritRate());
            vo.setComboRate(boss.getComboRate());
            vo.setDodgeRate(boss.getDodgeRate());
            vo.setBlockRate(boss.getBlockRate());
            vo.setLifesteal(boss.getLifesteal());
            // 抗性属性
            vo.setCritResistance(boss.getCritResistance());
            vo.setComboResistance(boss.getComboResistance());
            vo.setDodgeResistance(boss.getDodgeResistance());
            vo.setBlockResistance(boss.getBlockResistance());
            vo.setLifestealResistance(boss.getLifestealResistance());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BattleResultVO> battle(Long bossId) {
        // 参数校验
        if (bossId == null || bossId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Boss ID不能为空");
        }

        // 获取当前登录用户
        Long userId = userService.getLoginUser().getId();

        // 检查用户今天是否已达到挑战次数上限
        int remainingChallenges = getRemainingDailyChallenges(userId, bossId);
        if (remainingChallenges <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您今天已用完"+MAX_DAILY_CHALLENGES+"次挑战机会，请明天再来");
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

        // 从Redis获取Boss当前血量，检查是否已被击败
        int currentBossHealth = getBossHealthFromRedis(bossId, boss.getHealth());
        if (currentBossHealth <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该Boss已被击败，请明天再来挑战");
        }

        // 计算宠物的攻击力和血量
        int petLevel = pet.getLevel() != null ? pet.getLevel() : 1;
        
        // 获取用户的宠物装备属性
        PetEquipStatsVO petEquipStats = fishPetService.getPetEquipStats();
        int equipAttack = petEquipStats != null && petEquipStats.getTotalBaseAttack() != null 
                ? petEquipStats.getTotalBaseAttack() : 0;
        int equipDefense = petEquipStats != null && petEquipStats.getTotalBaseDefense() != null 
                ? petEquipStats.getTotalBaseDefense() : 0;
        int equipHp = petEquipStats != null && petEquipStats.getTotalBaseHp() != null 
                ? petEquipStats.getTotalBaseHp() : 0;
        double petCritRate = petEquipStats != null ? petEquipStats.getCritRate() : 0.0;
        double petComboRate = petEquipStats != null ? petEquipStats.getComboRate() : 0.0;
        double petDodgeRate = petEquipStats != null ? petEquipStats.getDodgeRate() : 0.0;
        
        // 攻击力 = 等级 + 装备攻击力
        int petAttack = petLevel + equipAttack;
        // 血量 = 等级 * 100 + 装备生命值
        int petHealth = petLevel * 100 + equipHp;
        // 防御力 = 装备防御力（用于减伤）
        int petDefense = equipDefense;

        // 初始化血量（宠物每次对战都是满血开始）
        int currentPetHealth = petHealth;

        // 创建对战结果列表
        List<BattleResultVO> battleResults = new ArrayList<>();
        
        // 初始化攻击顺序：第一回合宠物先攻击
        boolean petTurn = true;
        
        // 累计用户造成的总伤害
        int totalDamage = 0;

        // 统计双方各自已出手的有效回合数（连击不计入回合数）
        int petRounds = 0;
        int bossRounds = 0;

        // 为防止极端情况下无限连击导致死循环，增加一个最大行动次数保护
        int maxActions = 100;

        // 宠物和Boss各自最多出手 20回合（连击不算回合）
        while ((petRounds < 20 || bossRounds < 20)
                && currentPetHealth > 0
                && currentBossHealth > 0
                && maxActions-- > 0) {

            // 创建当前回合的对战结果对象
            BattleResultVO result = new BattleResultVO();

            // 根据当前回合决定攻击者（轮流攻击，除非连击）
            String attackerType = petTurn ? "PET" : "BOSS";
            result.setAttackerType(attackerType);

            // 执行一次攻击
            int damage;
            boolean isDodge;
            boolean isCritical;
            boolean isCombo;

            if (petTurn) {
                // 宠物攻击Boss - 使用宠物装备属性
                AttackResultVO attackResult = performAttack(petAttack, currentBossHealth,
                        petCritRate, petComboRate, 0.0); // Boss没有闪避率
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
                // Boss攻击宠物 - 使用宠物闪避属性和防御力减伤
                AttackResultVO attackResult = performAttack(boss.getAttack(), currentPetHealth,
                        0.0, 0.0, petDodgeRate, petDefense); // Boss没有暴击和连击，宠物有防御
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
            if ("PET".equals(attackerType)) {
                updateBossHealthToRedis(bossId, currentBossHealth);
            }

            // 如果不是连击，本次攻击计为一回合，并切换到另一方
            // 连击则仅追加伤害，不增加回合数，攻击方保持不变
            if (!isCombo) {
                if ("PET".equals(attackerType)) {
                    petRounds++;
                } else {
                    bossRounds++;
                }
                petTurn = !petTurn;
            }
        }

        // 增加用户今天挑战次数
        incrementDailyChallengeCount(userId, bossId);

        // 将用户造成的伤害存入排行榜
        updateBossChallengeRanking(bossId, userId, totalDamage);

        return battleResults;
    }

    /**
     * 执行攻击
     *
     * @param attackPower 攻击力
     * @param targetHealth 目标当前血量
     * @param critRate 暴击概率
     * @param comboRate 连击概率
     * @param dodgeRate 闪避概率
     * @param defense 防御力（仅用于受到攻击时减伤）
     * @return 攻击结果
     */
    private AttackResultVO performAttack(int attackPower, int targetHealth,
                                          double critRate, double comboRate, double dodgeRate, int defense) {
        AttackResultVO result = new AttackResultVO();

        // 判断是否闪避
        if (random.nextDouble() < dodgeRate) {
            result.setDodge(true);
            result.setDamage(0);
            return result;
        }

        // 判断是否连击
        boolean isCombo = random.nextDouble() < comboRate;
        result.setCombo(isCombo);

        // 判断是否暴击
        boolean isCritical = random.nextDouble() < critRate;
        result.setCritical(isCritical);

        // 计算伤害
        double damageMultiplier = 1.0;
        if (isCritical) {
            damageMultiplier *= CRITICAL_DAMAGE_MULTIPLIER;
        }
        if (isCombo) {
            damageMultiplier *= COMBO_DAMAGE_MULTIPLIER;
        }

        // 防御力减伤：每1点防御减少1点伤害，最低造成1点伤害
        int baseDamage = (int) (attackPower * damageMultiplier);
        int damageAfterDefense = Math.max(1, baseDamage - defense);

        // 伤害有10%的浮动 // 0.9-1.1
        double damageVariation = 0.9 + random.nextDouble() * 0.2;
        int damage = (int) (damageAfterDefense * damageVariation);

        // 确保伤害不超过目标当前血量
        damage = Math.min(damage, targetHealth);

        result.setDamage(damage);
        return result;
    }

    /**
     * 执行攻击（不带防御力参数的重载方法，用于宠物攻击Boss）
     *
     * @param attackPower 攻击力
     * @param targetHealth 目标当前血量
     * @param critRate 暴击概率
     * @param comboRate 连击概率
     * @param dodgeRate 闪避概率
     * @return 攻击结果
     */
    private AttackResultVO performAttack(int attackPower, int targetHealth,
                                          double critRate, double comboRate, double dodgeRate) {
        return performAttack(attackPower, targetHealth, critRate, comboRate, dodgeRate, 0);
    }

    /**
     * 根据ID获取Boss
     *
     * @param bossId Boss ID
     * @return Boss信息
     */
    private BossVO getBossById(Long bossId) {
        Boss boss = bossMapper.selectById(bossId);
        if (boss == null || boss.getIsDelete() == 1 || boss.getStatus() == 0) {
            return null;
        }
        BossVO vo = new BossVO();
        vo.setId(boss.getId());
        vo.setName(boss.getName());
        vo.setAvatar(boss.getAvatar());
        vo.setHealth(boss.getHealth());
        vo.setAttack(boss.getAttack());
        vo.setRewardPoints(boss.getRewardPoints());
        // 主动属性
        vo.setCritRate(boss.getCritRate());
        vo.setComboRate(boss.getComboRate());
        vo.setDodgeRate(boss.getDodgeRate());
        vo.setBlockRate(boss.getBlockRate());
        vo.setLifesteal(boss.getLifesteal());
        // 抗性属性
        vo.setCritResistance(boss.getCritResistance());
        vo.setComboResistance(boss.getComboResistance());
        vo.setDodgeResistance(boss.getDodgeResistance());
        vo.setBlockResistance(boss.getBlockResistance());
        vo.setLifestealResistance(boss.getLifestealResistance());
        return vo;
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
     * 获取用户今天剩余的挑战次数
     *
     * @param userId 用户ID
     * @param bossId Boss ID
     * @return 剩余挑战次数
     */
    private int getRemainingDailyChallenges(Long userId, Long bossId) {
        try {
            String dateStr = LocalDate.now().toString();
            String battleKey = RedisKey.getKey(RedisKey.BOSS_BATTLE_USER_DAILY_KEY, userId, bossId, dateStr);
            String value = redisTemplate.opsForValue().get(battleKey);
            
            int usedChallenges = 0;
            if (value != null && !value.isEmpty()) {
                usedChallenges = Integer.parseInt(value);
            }
            
            return MAX_DAILY_CHALLENGES - usedChallenges;
        } catch (Exception e) {
            log.error("获取用户Boss挑战次数失败，userId: {}, bossId: {}", userId, bossId, e);
            // 如果检查失败，为了安全起见，返回0（不允许挑战）
            return 0;
        }
    }

    /**
     * 增加用户今天挑战次数
     *
     * @param userId 用户ID
     * @param bossId Boss ID
     */
    private void incrementDailyChallengeCount(Long userId, Long bossId) {
        try {
            String dateStr = LocalDate.now().toString();
            String battleKey = RedisKey.getKey(RedisKey.BOSS_BATTLE_USER_DAILY_KEY, userId, bossId, dateStr);
            
            // 获取当前次数并+1
            String value = redisTemplate.opsForValue().get(battleKey);
            int currentCount = 0;
            if (value != null && !value.isEmpty()) {
                currentCount = Integer.parseInt(value);
            }
            currentCount++;
            
            // 计算到明天凌晨的过期时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextDayMidnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            long expireSeconds = ChronoUnit.SECONDS.between(now, nextDayMidnight);
            
            redisTemplate.opsForValue().set(battleKey, String.valueOf(currentCount), expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("增加用户Boss挑战次数失败，userId: {}, bossId: {}", userId, bossId, e);
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
        
        // 获取用户的宠物装备属性
        PetEquipStatsVO petEquipStats = fishPetService.getPetEquipStats();
        int equipAttack = petEquipStats != null && petEquipStats.getTotalBaseAttack() != null 
                ? petEquipStats.getTotalBaseAttack() : 0;
        int equipHp = petEquipStats != null && petEquipStats.getTotalBaseHp() != null 
                ? petEquipStats.getTotalBaseHp() : 0;
        
        // 攻击力 = 等级 + 装备攻击力
        int petAttack = petLevel + equipAttack;
        // 血量 = 等级 * 100 + 装备生命值
        int petHealth = petLevel * 100 + equipHp;

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
        // 获取宠物已穿戴的装备列表
        petInfo.setEquippedItems(fishPetService.getEquippedItems(pet));
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

    /**
     * 分配Boss击败奖励
     * 按排行榜排名百分比分配Boss总积分
     *
     * @param bossId Boss ID
     * @param totalRewardPoints Boss总积分
     */
    @Override
    public void distributeBossKillRewards(Long bossId, Integer totalRewardPoints) {
        try {
            // 检查是否已发放过奖励，防止重复发放
            String rewardDistributedKey = RedisKey.getKey(RedisKey.BOSS_REWARD_DISTRIBUTED_KEY, bossId);
            Boolean alreadyDistributed = redisTemplate.opsForValue().setIfAbsent(
                    rewardDistributedKey, "1", BOSS_HEALTH_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(alreadyDistributed)) {
                log.info("Boss击败奖励已发放过，跳过，bossId: {}", bossId);
                return;
            }
            
            log.info("开始分配Boss击败奖励，bossId: {}, 总积分: {}", bossId, totalRewardPoints);
            
            String rankingKey = RedisKey.getKey(RedisKey.BOSS_CHALLENGE_RANKING_KEY, bossId);
            
            // 获取所有排行榜参与者（从第11名开始）
            Set<ZSetOperations.TypedTuple<String>> allTuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(rankingKey, 0, -1);
            
            if (allTuples == null || allTuples.isEmpty()) {
                log.warn("Boss击败时排行榜为空，bossId: {}", bossId);
                return;
            }
            
            int rank = 0;
            for (ZSetOperations.TypedTuple<String> tuple : allTuples) {
                Long userId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
                double percentage;
                
                if (rank < BOSS_REWARD_PERCENTAGES.length) {
                    // 前10名按百分比分配
                    percentage = BOSS_REWARD_PERCENTAGES[rank];
                } else {
                    // 第11名及以后各得2%
                    percentage = OTHER_PARTICIPANTS_REWARD_PERCENTAGE;
                }
                
                int rewardPoints = (int) (totalRewardPoints * percentage);
                
                // 发放积分奖励
                try {
                    String description;
                    if (rank < BOSS_REWARD_PERCENTAGES.length) {
                        description = "击败Boss获得第" + (rank + 1) + "名奖励";
                    } else {
                        description = "击败Boss获得参与奖励（第" + (rank + 1) + "名）";
                    }
                    
                    userPointsService.updateUsedPoints(userId, -rewardPoints, 
                            "BOSS_KILL", bossId.toString(), description);
                    log.info("Boss击败奖励发放成功，userId: {}, rank: {}, rewardPoints: {}", 
                            userId, rank + 1, rewardPoints);
                } catch (Exception e) {
                    log.error("Boss击败奖励发放失败，userId: {}, rank: {}", userId, rank + 1, e);
                }
                
                rank++;
            }
            
            log.info("Boss击败奖励分配完成，bossId: {}，共{}人获得奖励", bossId, rank);
        } catch (Exception e) {
            log.error("分配Boss击败奖励失败，bossId: {}, totalRewardPoints: {}", bossId, totalRewardPoints, e);
        }
    }
}
