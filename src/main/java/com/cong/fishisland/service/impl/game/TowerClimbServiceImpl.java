package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.mapper.game.TowerClimbProgressMapper;
import com.cong.fishisland.mapper.game.TowerClimbRecordMapper;
import com.cong.fishisland.model.entity.game.TowerClimbProgress;
import com.cong.fishisland.model.entity.game.TowerClimbRecord;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.game.AttackResultVO;
import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
import com.cong.fishisland.model.vo.game.TowerClimbResultVO;
import com.cong.fishisland.model.vo.game.TowerFloorMonsterVO;
import com.cong.fishisland.model.vo.game.TowerProgressVO;
import com.cong.fishisland.model.vo.game.TowerRankVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.TowerClimbService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 爬塔服务实现类
 * <p>
 * 怪物属性随层数无限递增，公式：
 * - 血量 = BASE_HP * floor^HP_SCALE
 * - 攻击 = BASE_ATK * floor^ATK_SCALE
 * - 概率属性随层数缓慢增长，上限 0.5
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TowerClimbServiceImpl implements TowerClimbService {

    private final UserService userService;
    private final FishPetService fishPetService;
    private final UserPointsService userPointsService;
    private final TowerClimbRecordMapper towerClimbRecordMapper;
    private final TowerClimbProgressMapper towerClimbProgressMapper;
    private final Random random = new Random();

    // 怪物基础属性
    private static final int MONSTER_BASE_HP = 200;
    private static final int MONSTER_BASE_ATK = 15;
    // 属性成长指数（floor^scale）
    private static final double HP_SCALE = 1.4;
    private static final double ATK_SCALE = 1.3;
    // 概率属性每层增长量（上限 2.0）
    private static final double PROB_GROWTH_PER_FLOOR = 0.006;
    private static final double PROB_CAP = 2.0;
    // 每层基础奖励积分
    private static final int BASE_REWARD = 5;
    // 最大战斗回合数
    private static final int MAX_ROUNDS = 30;

    @Override
    public TowerProgressVO getProgress() {
        Long userId = userService.getLoginUser().getId();
        TowerClimbProgress progress = getOrCreateProgress(userId);
        int nextFloor = progress.getMaxFloor() + 1;

        TowerProgressVO vo = new TowerProgressVO();
        vo.setMaxFloor(progress.getMaxFloor());
        vo.setNextFloor(nextFloor);
        vo.setNextMonster(getFloorMonster(nextFloor));
        return vo;
    }

    @Override
    public TowerFloorMonsterVO getFloorMonster(int floor) {
        if (floor < 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "层数不能小于1");
        }
        TowerFloorMonsterVO vo = new TowerFloorMonsterVO();
        vo.setFloor(floor);
        vo.setName("第 " + floor + " 层守卫");
        vo.setHealth(calcMonsterHp(floor));
        vo.setAttack(calcMonsterAtk(floor));
        vo.setCritRate(calcProbAttr(floor, 3));
        vo.setComboRate(calcProbAttr(floor, 5));
        vo.setDodgeRate(calcProbAttr(floor, 8));
        vo.setBlockRate(calcProbAttr(floor, 4));
        vo.setLifesteal(calcProbAttr(floor, 6));
        vo.setRewardPoints(BASE_REWARD * floor);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TowerClimbResultVO challenge() {
        Long userId = userService.getLoginUser().getId();
        TowerClimbProgress progress = getOrCreateProgress(userId);
        int floor = progress.getMaxFloor() + 1;

        // 获取宠物属性
        FishPet pet = getPet(userId);
        PetEquipStatsVO equipStats = fishPetService.getPetEquipStats();
        int petLevel = pet.getLevel() != null ? pet.getLevel() : 1;

        BattleStatsVO petStats = BattleStatsVO.fromPet(petLevel, equipStats,
                BattleConstant.BASE_ATK, BattleConstant.GROWTH_RATE);

        // 构建怪物属性
        TowerFloorMonsterVO monster = getFloorMonster(floor);
        BattleStatsVO monsterStats = buildMonsterStats(monster);

        // 执行战斗
        List<BattleResultVO> rounds = doBattle(petStats, monsterStats);

        // 判断胜负（宠物血量 > 0 则胜利）
        int lastPetHp = rounds.get(rounds.size() - 1).getPetRemainingHealth();
        boolean win = lastPetHp > 0;

        int rewardPoints = 0;
        if (win) {
            rewardPoints = monster.getRewardPoints();
            // 更新最高层数
            progress.setMaxFloor(floor);
            towerClimbProgressMapper.updateById(progress);
            // 发放积分
            userPointsService.updatePoints(userId, rewardPoints, false);
        }

        // 保存挑战记录
        TowerClimbRecord record = new TowerClimbRecord();
        record.setUserId(userId);
        record.setFloor(floor);
        record.setMaxFloor(win ? floor : progress.getMaxFloor());
        record.setResult(win ? 1 : 0);
        record.setPetLevel(petLevel);
        record.setPetHpLeft(lastPetHp);
        record.setRewardPoints(rewardPoints);
        towerClimbRecordMapper.insert(record);

        TowerClimbResultVO result = new TowerClimbResultVO();
        result.setFloor(floor);
        result.setWin(win);
        result.setPetHpLeft(lastPetHp);
        result.setRewardPoints(rewardPoints);
        result.setMaxFloor(win ? floor : progress.getMaxFloor());
        result.setBattleRounds(rounds);
        return result;
    }

    // ---- 战斗逻辑 ----

    private List<BattleResultVO> doBattle(BattleStatsVO pet, BattleStatsVO monster) {
        List<BattleResultVO> rounds = new ArrayList<>();
        int petHp = pet.getHealth();
        int monsterHp = monster.getHealth();
        boolean petTurn = true; // 宠物先手

        for (int i = 0; i < MAX_ROUNDS && petHp > 0 && monsterHp > 0; i++) {
            BattleResultVO round = new BattleResultVO();
            if (petTurn) {
                round.setAttackerType("PET");
                AttackResultVO atk = performAttack(pet, monster, petHp, monsterHp);
                monsterHp = Math.max(0, monsterHp - atk.getDamage());
                petHp = Math.min(pet.getHealth(), petHp + atk.getLifestealHeal());
                round.setDamage(atk.getDamage());
                round.setIsCritical(atk.isCritical());
                round.setIsCombo(atk.isCombo());
                round.setIsBlock(atk.isBlock());
                round.setIsDodge(atk.isDodge());
                round.setIsNormalAttack(!atk.isCritical() && !atk.isCombo() && !atk.isDodge());
                round.setLifestealHeal(atk.getLifestealHeal());
                // 连击时宠物继续攻击，否则切换到怪物
                petTurn = atk.isCombo();
            } else {
                round.setAttackerType("BOSS");
                AttackResultVO atk = performAttack(monster, pet, monsterHp, petHp);
                petHp = Math.max(0, petHp - atk.getDamage());
                monsterHp = Math.min(monster.getHealth(), monsterHp + atk.getLifestealHeal());
                round.setDamage(atk.getDamage());
                round.setIsCritical(atk.isCritical());
                round.setIsCombo(atk.isCombo());
                round.setIsBlock(atk.isBlock());
                round.setIsDodge(atk.isDodge());
                round.setIsNormalAttack(!atk.isCritical() && !atk.isCombo() && !atk.isDodge());
                round.setLifestealHeal(atk.getLifestealHeal());
                petTurn = !atk.isCombo();
            }
            round.setPetRemainingHealth(petHp);
            round.setBossRemainingHealth(monsterHp);
            rounds.add(round);
        }
        return rounds;
    }

    private AttackResultVO performAttack(BattleStatsVO attacker, BattleStatsVO defender,
                                          int attackerHp, int defenderHp) {
        AttackResultVO result = new AttackResultVO();

        double effectiveDodge = Math.max(0.0, defender.getDodgeRate() - attacker.getDodgeResistance());
        if (random.nextDouble() < effectiveDodge) {
            result.setDodge(true);
            result.setDamage(0);
            return result;
        }

        double effectiveBlock = Math.max(0.0, defender.getBlockRate() - attacker.getBlockResistance());
        boolean isBlock = random.nextDouble() < effectiveBlock;
        result.setBlock(isBlock);

        double effectiveCrit = Math.max(0.0, attacker.getCritRate() - defender.getCritResistance());
        boolean isCrit = random.nextDouble() < effectiveCrit;
        result.setCritical(isCrit);

        double effectiveCombo = Math.max(0.0, attacker.getComboRate() - defender.getComboResistance());
        boolean isCombo = random.nextDouble() < effectiveCombo;
        result.setCombo(isCombo);

        double multiplier = 1.0;
        if (isCrit) multiplier *= BattleConstant.CRITICAL_DAMAGE_MULTIPLIER;
        if (isCombo) multiplier *= BattleConstant.COMBO_DAMAGE_MULTIPLIER;
        if (isBlock) multiplier *= BattleConstant.BLOCK_DAMAGE_REDUCTION;

        int base = (int) (attacker.getAttack() * multiplier);
        int afterDef = Math.max(1, base - defender.getDefense());
        double variation = 0.9 + random.nextDouble() * 0.2;
        int damage = Math.min((int) (afterDef * variation), defenderHp);
        result.setDamage(damage);

        double effectiveLifesteal = Math.max(0.0, attacker.getLifesteal() - defender.getLifestealResistance());
        if (effectiveLifesteal > 0 && damage > 0) {
            int heal = (int) (damage * effectiveLifesteal);
            int missing = attacker.getHealth() - attackerHp;
            result.setLifestealHeal(Math.min(heal, missing));
        }
        return result;
    }

    // ---- 属性计算 ----

    /** 怪物血量：随层数指数增长 */
    private int calcMonsterHp(int floor) {
        return (int) (MONSTER_BASE_HP * Math.pow(floor, HP_SCALE));
    }

    /** 怪物攻击：随层数指数增长 */
    private int calcMonsterAtk(int floor) {
        return (int) (MONSTER_BASE_ATK * Math.pow(floor, ATK_SCALE));
    }

    /**
     * 概率属性随层数缓慢增长，startFloor 为开始增长的层数，上限 PROB_CAP
     */
    private double calcProbAttr(int floor, int startFloor) {
        if (floor <= startFloor) return 0.0;
        return Math.min(PROB_CAP, (floor - startFloor) * PROB_GROWTH_PER_FLOOR);
    }

    private BattleStatsVO buildMonsterStats(TowerFloorMonsterVO monster) {
        BattleStatsVO s = new BattleStatsVO();
        s.setAttack(monster.getAttack());
        s.setHealth(monster.getHealth());
        s.setDefense(0);
        s.setCritRate(monster.getCritRate());
        s.setComboRate(monster.getComboRate());
        s.setDodgeRate(monster.getDodgeRate());
        s.setBlockRate(monster.getBlockRate());
        s.setLifesteal(monster.getLifesteal());
        return s;
    }

    // ---- 工具方法 ----

    private TowerClimbProgress getOrCreateProgress(Long userId) {
        TowerClimbProgress progress = towerClimbProgressMapper.selectOne(
                new LambdaQueryWrapper<TowerClimbProgress>().eq(TowerClimbProgress::getUserId, userId));
        if (progress == null) {
            progress = new TowerClimbProgress();
            progress.setUserId(userId);
            progress.setMaxFloor(0);
            towerClimbProgressMapper.insert(progress);
        }
        return progress;
    }

    private FishPet getPet(Long userId) {
        FishPet pet = fishPetService.getOne(
                new LambdaQueryWrapper<FishPet>().eq(FishPet::getUserId, userId));
        if (pet == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请先领养宠物再挑战爬塔");
        }
        return pet;
    }

    @Override
    public List<TowerRankVO> getRanking(int limit) {
        // 按最高层数降序取 top N
        List<TowerClimbProgress> progressList = towerClimbProgressMapper.selectList(
                new LambdaQueryWrapper<TowerClimbProgress>()
                        .gt(TowerClimbProgress::getMaxFloor, 0)
                        .orderByDesc(TowerClimbProgress::getMaxFloor)
                        .last("LIMIT " + Math.min(limit, 200)));

        if (progressList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查用户信息
        List<Long> userIds = progressList.stream()
                .map(TowerClimbProgress::getUserId)
                .collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 组装 VO
        List<TowerRankVO> result = new ArrayList<>();
        for (int i = 0; i < progressList.size(); i++) {
            TowerClimbProgress p = progressList.get(i);
            User user = userMap.get(p.getUserId());
            TowerRankVO vo = new TowerRankVO();
            vo.setRank(i + 1);
            vo.setUserId(p.getUserId());
            vo.setMaxFloor(p.getMaxFloor());
            if (user != null) {
                vo.setUserName(user.getUserName());
                vo.setUserAvatar(user.getUserAvatar());
            }
            result.add(vo);
        }
        return result;
    }
}
