package com.cong.fishisland.service.impl.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.BattleConstant;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.vo.game.BattleStatsVO;
import com.cong.fishisland.model.vo.game.PetBattleInfoVO;
import com.cong.fishisland.model.vo.game.PetBattleResultVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetBattleService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.pet.PetBattleSimulation;
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

        PetEquipStatsVO myStats = fishPetService.getPetEquipStatsByPet(myPet);
        PetEquipStatsVO opponentStats = fishPetService.getPetEquipStatsByPet(opponentPet);

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
                fishPetService.getPetEquipStatsByPet(myPet),
                BattleConstant.BASE_ATK, BattleConstant.GROWTH_RATE);

        BattleStatsVO opp = BattleStatsVO.fromPet(
                opponentPet.getLevel() != null ? opponentPet.getLevel() : 1,
                fishPetService.getPetEquipStatsByPet(opponentPet),
                BattleConstant.BASE_ATK, BattleConstant.GROWTH_RATE);

        PetBattleSimulation battle = new PetBattleSimulation(my, opp, opp.getHealth(), true, random);
        List<PetBattleResultVO> results = new ArrayList<>();
        while (results.size() < 100 && battle.hasNext()) {
            results.add(battle.next().toDuelRound());
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

}
