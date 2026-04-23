package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.game.AttackResultVO;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
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

        BattleStatsVO my = BattleStatsVO.fromPet(
                myPet.getLevel() != null ? myPet.getLevel() : 1,
                fishPetService.getPetEquipStats(),
                BattleConstant.BASE_ATK, BattleConstant.GROWTH_RATE);

        BattleStatsVO opp = BattleStatsVO.fromPet(
                opponentPet.getLevel() != null ? opponentPet.getLevel() : 1,
                fishPetService.getPetEquipStatsByUserId(opponentUserId),
                BattleConstant.BASE_ATK, BattleConstant.GROWTH_RATE);

        int currentMyHealth = my.getHealth();
        int currentOppHealth = opp.getHealth();

        List<PetBattleResultVO> results = new ArrayList<>();
        boolean myTurn = true;
        int maxActions = 100;

        while (currentMyHealth > 0 && currentOppHealth > 0 && maxActions-- > 0) {

            PetBattleResultVO result = new PetBattleResultVO();
            result.setAttackerType(myTurn ? "MY_PET" : "OPPONENT_PET");

            AttackResultVO ar;
            if (myTurn) {
                ar = performAttack(my, opp, currentMyHealth, currentOppHealth);
                if (!ar.isDodge()) {
                    currentOppHealth = Math.max(0, currentOppHealth - ar.getDamage());
                    currentMyHealth = Math.min(my.getHealth(), currentMyHealth + ar.getLifestealHeal());
                }
            } else {
                ar = performAttack(opp, my, currentOppHealth, currentMyHealth);
                if (!ar.isDodge()) {
                    currentMyHealth = Math.max(0, currentMyHealth - ar.getDamage());
                    currentOppHealth = Math.min(opp.getHealth(), currentOppHealth + ar.getLifestealHeal());
                }
            }

            result.setDamage(ar.getDamage());
            result.setIsDodge(ar.isDodge());
            result.setIsCritical(ar.isCritical());
            result.setIsCombo(ar.isCombo());
            result.setIsBlock(ar.isBlock());
            result.setLifestealHeal(ar.getLifestealHeal());
            result.setIsNormalAttack(!ar.isDodge() && !ar.isCritical() && !ar.isCombo() && !ar.isBlock());
            result.setMyPetRemainingHealth(currentMyHealth);
            result.setOpponentPetRemainingHealth(currentOppHealth);
            results.add(result);

            if (!ar.isCombo()) {
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

    private PetBattleInfoVO.PetInfo buildPetInfo(FishPet pet, PetEquipStatsVO stats) {
        BattleStatsVO s = BattleStatsVO.fromPet(
                pet.getLevel() != null ? pet.getLevel() : 1,
                stats, BattleConstant.BASE_ATK, BattleConstant.GROWTH_RATE);
        PetBattleInfoVO.PetInfo info = new PetBattleInfoVO.PetInfo();
        info.setPetId(pet.getPetId());
        info.setUserId(pet.getUserId());
        info.setName(pet.getName());
        info.setAvatar(pet.getPetUrl());
        info.setLevel(pet.getLevel() != null ? pet.getLevel() : 1);
        info.setAttack(s.getAttack());
        info.setHealth(s.getHealth());
        info.setEquippedItems(fishPetService.getEquippedItems(pet));
        return info;
    }

    /**
     * 执行一次攻击：attacker 攻击 defender
     *
     * @param attacker          攻击方战斗属性
     * @param defender          防守方战斗属性
     * @param attackerCurrentHp 攻击方当前血量（用于吸血上限）
     * @param defenderCurrentHp 防守方当前血量（用于伤害上限）
     */
    private AttackResultVO performAttack(BattleStatsVO attacker, BattleStatsVO defender,
                                         int attackerCurrentHp, int defenderCurrentHp) {
        AttackResultVO result = new AttackResultVO();

        // 闪避：防守方闪避率 - 攻击方抗闪避率
        double effectiveDodge = Math.max(0.0, defender.getDodgeRate() - attacker.getDodgeResistance());
        if (random.nextDouble() < effectiveDodge) {
            result.setDodge(true);
            result.setDamage(0);
            return result;
        }

        // 格挡：防守方格挡率 - 攻击方抗格挡率（触发后减伤50%）
        double effectiveBlock = Math.max(0.0, defender.getBlockRate() - attacker.getBlockResistance());
        boolean isBlock = random.nextDouble() < effectiveBlock;
        result.setBlock(isBlock);

        // 暴击：攻击方暴击率 - 防守方抗暴击率
        double effectiveCrit = Math.max(0.0, attacker.getCritRate() - defender.getCritResistance());
        boolean isCritical = random.nextDouble() < effectiveCrit;

        // 连击：攻击方连击率 - 防守方抗连击率
        double effectiveCombo = Math.max(0.0, attacker.getComboRate() - defender.getComboResistance());
        boolean isCombo = random.nextDouble() < effectiveCombo;

        result.setCritical(isCritical);
        result.setCombo(isCombo);

        double multiplier = 1.0;
        if (isCritical) {
            multiplier *= BattleConstant.CRITICAL_DAMAGE_MULTIPLIER;
        }
        if (isCombo) {
            multiplier *= BattleConstant.COMBO_DAMAGE_MULTIPLIER;
        }
        if (isBlock) {
            multiplier *= BattleConstant.BLOCK_DAMAGE_REDUCTION;
        }

        int base = (int) (attacker.getAttack() * multiplier);
        int afterDefense = Math.max(1, base - defender.getDefense());
        double variation = 0.9 + random.nextDouble() * 0.2;
        int damage = Math.min((int) (afterDefense * variation), defenderCurrentHp);
        result.setDamage(damage);

        // 吸血：攻击方吸血率 - 防守方抗吸血率，回复量不超过缺失血量
        double effectiveLifesteal = Math.max(0.0, attacker.getLifesteal() - defender.getLifestealResistance());
        if (effectiveLifesteal > 0 && damage > 0) {
            int heal = (int) (damage * effectiveLifesteal);
            int missing = attacker.getHealth() - attackerCurrentHp;
            result.setLifestealHeal(Math.min(heal, missing));
        }

        return result;
    }
}
