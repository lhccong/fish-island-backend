package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.game.AttackResultVO;
import com.cong.fishisland.model.vo.game.PetBattleInfoVO;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetBattleService;
import com.cong.fishisland.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 宠物对战服务实现类
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetBattleServiceImpl implements PetBattleService {

    private final UserService userService;
    private final FishPetService fishPetService;
    private final Random random = new Random();

    // 暴击伤害倍数
    private static final double CRITICAL_DAMAGE_MULTIPLIER = BattleConstant.CRITICAL_DAMAGE_MULTIPLIER;
    // 连击伤害倍数
    private static final double COMBO_DAMAGE_MULTIPLIER = BattleConstant.COMBO_DAMAGE_MULTIPLIER;

    @Override
    public PetBattleInfoVO getPetBattleInfo(Long opponentUserId) {
        if (opponentUserId == null || opponentUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对手用户ID不能为空");
        }

        Long myUserId = userService.getLoginUser().getId();
        if (myUserId.equals(opponentUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能与自己对战");
        }

        FishPet myPet = getPetByUserId(myUserId, true);
        FishPet opponentPet = getPetByUserId(opponentUserId, false);

        PetEquipStatsVO myStats = fishPetService.getPetEquipStats();
        PetEquipStatsVO opponentStats = fishPetService.getPetEquipStatsByUserId(opponentUserId);

        PetBattleInfoVO vo = new PetBattleInfoVO();
        vo.setMyPet(buildPetInfo(myPet, myStats));
        vo.setOpponentPet(buildPetInfo(opponentPet, opponentStats));
        return vo;
    }

    @Override
    public List<PetBattleResultVO> battle(Long opponentUserId) {
        if (opponentUserId == null || opponentUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对手用户ID不能为空");
        }

        Long myUserId = userService.getLoginUser().getId();
        if (myUserId.equals(opponentUserId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能与自己对战");
        }

        FishPet myPet = getPetByUserId(myUserId, true);
        FishPet opponentPet = getPetByUserId(opponentUserId, false);

        // 我方属性（含装备）
        PetEquipStatsVO myStats = fishPetService.getPetEquipStats();
        int myAttack = calcAttack(myPet.getLevel(), myStats);
        int myHealth = calcHealth(myPet.getLevel(), myStats);
        int myDefense = myStats != null && myStats.getTotalBaseDefense() != null ? myStats.getTotalBaseDefense() : 0;
        double myCritRate = myStats != null && myStats.getCritRate() != null ? myStats.getCritRate() : 0.0;
        double myComboRate = myStats != null && myStats.getComboRate() != null ? myStats.getComboRate() : 0.0;
        double myDodgeRate = myStats != null && myStats.getDodgeRate() != null ? myStats.getDodgeRate() : 0.0;

        // 对手属性（含装备，通过userId获取）
        PetEquipStatsVO opponentStats = fishPetService.getPetEquipStatsByUserId(opponentUserId);
        int opponentAttack = calcAttack(opponentPet.getLevel(), opponentStats);
        int opponentHealth = calcHealth(opponentPet.getLevel(), opponentStats);
        int opponentDefense = opponentStats != null && opponentStats.getTotalBaseDefense() != null ? opponentStats.getTotalBaseDefense() : 0;
        double opponentCritRate = opponentStats != null && opponentStats.getCritRate() != null ? opponentStats.getCritRate() : 0.0;
        double opponentComboRate = opponentStats != null && opponentStats.getComboRate() != null ? opponentStats.getComboRate() : 0.0;
        double opponentDodgeRate = opponentStats != null && opponentStats.getDodgeRate() != null ? opponentStats.getDodgeRate() : 0.0;

        int currentMyHealth = myHealth;
        int currentOpponentHealth = opponentHealth;

        List<PetBattleResultVO> results = new ArrayList<>();
        boolean myTurn = true;
        int maxActions = 100;

        while (currentMyHealth > 0
                && currentOpponentHealth > 0
                && maxActions-- > 0) {

            PetBattleResultVO result = new PetBattleResultVO();
            String attackerType = myTurn ? "MY_PET" : "OPPONENT_PET";
            result.setAttackerType(attackerType);

            AttackResultVO attackResult;
            if (myTurn) {
                attackResult = performAttack(myAttack, currentOpponentHealth,
                        myCritRate, myComboRate, opponentDodgeRate, opponentDefense);
                if (!attackResult.isDodge()) {
                    currentOpponentHealth = Math.max(0, currentOpponentHealth - attackResult.getDamage());
                }
            } else {
                attackResult = performAttack(opponentAttack, currentMyHealth,
                        opponentCritRate, opponentComboRate, myDodgeRate, myDefense);
                if (!attackResult.isDodge()) {
                    currentMyHealth = Math.max(0, currentMyHealth - attackResult.getDamage());
                }
            }

            result.setDamage(attackResult.getDamage());
            result.setIsDodge(attackResult.isDodge());
            result.setIsCritical(attackResult.isCritical());
            result.setIsCombo(attackResult.isCombo());
            result.setIsNormalAttack(!attackResult.isDodge() && !attackResult.isCritical() && !attackResult.isCombo());
            result.setMyPetRemainingHealth(currentMyHealth);
            result.setOpponentPetRemainingHealth(currentOpponentHealth);

            results.add(result);

            if (!attackResult.isCombo()) {
                myTurn = !myTurn;
            }
        }

        return results;
    }

    // ---- 私有工具方法 ----

    private FishPet getPetByUserId(Long userId, boolean isSelf) {
        QueryWrapper<FishPet> qw = new QueryWrapper<>();
        qw.eq("userId", userId);
        FishPet pet = fishPetService.getOne(qw);
        if (pet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, isSelf ? "您还没有宠物" : "对手还没有宠物");
        }
        return pet;
    }

    private int calcAttack(Integer level, PetEquipStatsVO stats) {
        int lv = level != null ? level : 1;
        int equipAtk = stats != null && stats.getTotalBaseAttack() != null ? stats.getTotalBaseAttack() : 0;
        int baseAtk = (int) (BattleConstant.BASE_ATK * Math.pow(1 + BattleConstant.GROWTH_RATE, lv));
        return baseAtk + equipAtk;
    }

    private int calcHealth(Integer level, PetEquipStatsVO stats) {
        int lv = level != null ? level : 1;
        int equipHp = stats != null && stats.getTotalBaseHp() != null ? stats.getTotalBaseHp() : 0;
        return lv * 100 + equipHp;
    }

    private PetBattleInfoVO.PetInfo buildPetInfo(FishPet pet, PetEquipStatsVO stats) {
        PetBattleInfoVO.PetInfo info = new PetBattleInfoVO.PetInfo();
        info.setPetId(pet.getPetId());
        info.setUserId(pet.getUserId());
        info.setName(pet.getName());
        info.setAvatar(pet.getPetUrl());
        info.setLevel(pet.getLevel() != null ? pet.getLevel() : 1);
        info.setAttack(calcAttack(pet.getLevel(), stats));
        info.setHealth(calcHealth(pet.getLevel(), stats));
        info.setEquippedItems(fishPetService.getEquippedItems(pet));
        return info;
    }

    private AttackResultVO performAttack(int attackPower, int targetHealth,
                                         double critRate, double comboRate,
                                         double dodgeRate, int defense) {
        AttackResultVO result = new AttackResultVO();

        if (random.nextDouble() < dodgeRate) {
            result.setDodge(true);
            result.setDamage(0);
            return result;
        }

        boolean isCombo = random.nextDouble() < comboRate;
        boolean isCritical = random.nextDouble() < critRate;
        result.setCombo(isCombo);
        result.setCritical(isCritical);

        double multiplier = 1.0;
        if (isCritical) multiplier *= CRITICAL_DAMAGE_MULTIPLIER;
        if (isCombo) multiplier *= COMBO_DAMAGE_MULTIPLIER;

        int base = (int) (attackPower * multiplier);
        int afterDefense = Math.max(1, base - defense);
        double variation = 0.9 + random.nextDouble() * 0.2;
        int damage = Math.min((int) (afterDefense * variation), targetHealth);

        result.setDamage(damage);
        return result;
    }
}
