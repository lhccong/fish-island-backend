package com.cong.fishisland.service.impl.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.PetEquipForgeMapper;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.entity.pet.EquipEntry;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.pet.PetEquipForge;
import com.cong.fishisland.model.enums.pet.EntryAttrEnum;
import com.cong.fishisland.model.enums.pet.EntryGradeEnum;
import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.pet.PetEquipForgeVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.service.PetEquipForgeService;
import com.cong.fishisland.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 宠物装备锻造服务实现
 *
 * @author cong
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetEquipForgeServiceImpl extends ServiceImpl<PetEquipForgeMapper, PetEquipForge>
        implements PetEquipForgeService {

    private final FishPetService fishPetService;
    private final UserPointsService userPointsService;

    /** 基础刷新消耗积分 */
    private static final int BASE_REFRESH_COST = 100;
    /** 每锁定一条词条额外消耗积分 */
    private static final int LOCK_EXTRA_COST = 50;
    /** 基础升级消耗积分（每级递增） */
    private static final int BASE_UPGRADE_COST = 50;
    /** 升级积分递增系数（等级 * 系数） */
    private static final int UPGRADE_COST_FACTOR = 20;
    /** 最大装备等级 */
    private static final int MAX_EQUIP_LEVEL = 20;

    /**
     * 词条属性列表，用于随机
     */
    private static final EntryAttrEnum[] ALL_ATTRS = EntryAttrEnum.values();

    @Override
    public List<PetEquipForgeVO> listByPetId(Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();
        getPetAndCheckOwner(petId, userId);

        List<PetEquipForge> list = lambdaQuery()
                .eq(PetEquipForge::getPetId, petId)
                .list();

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetEquipForgeVO refreshEntries(ForgeRefreshRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        FishPet pet = getPetAndCheckOwner(request.getPetId(), userId);

        EquipSlotEnum slot = EquipSlotEnum.of(request.getEquipSlot());
        List<Integer> lockedEntries = request.getLockedEntries() == null
                ? Collections.emptyList() : request.getLockedEntries();

        // 计算积分消耗：基础 100 + 每锁定一条 50
        int lockedCount = (int) lockedEntries.stream()
                .filter(i -> i >= 1 && i <= 4).count();
        int cost = BASE_REFRESH_COST + lockedCount * LOCK_EXTRA_COST;

        // 扣除积分
        userPointsService.deductPoints(userId, cost,
                PointsRecordSourceEnum.OTHER.getValue(),
                String.valueOf(request.getPetId()),
                "宠物装备词条刷新-" + slot.getLabel());

        // 查询或初始化装备记录
        PetEquipForge forge = getOrCreateForge(pet, slot);

        // 获取当前词条（用于保留锁定词条）
        EquipEntry[] current = {forge.getEntry1(), forge.getEntry2(), forge.getEntry3(), forge.getEntry4()};

        // 刷新未锁定的词条
        for (int i = 1; i <= 4; i++) {
            if (!lockedEntries.contains(i)) {
                current[i - 1] = randomEntry();
            } else {
                // 保留锁定词条，但更新 locked 标记
                if (current[i - 1] != null) {
                    current[i - 1].setLocked(true);
                }
            }
        }

        // 刷新后所有词条 locked 重置为 false（锁定只在本次刷新生效）
        for (EquipEntry entry : current) {
            if (entry != null) entry.setLocked(false);
        }

        forge.setEntry1(current[0]);
        forge.setEntry2(current[1]);
        forge.setEntry3(current[2]);
        forge.setEntry4(current[3]);
        updateById(forge);

        return toVO(forge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean upgradeEquip(ForgeUpgradeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        FishPet pet = getPetAndCheckOwner(request.getPetId(), userId);

        EquipSlotEnum slot = EquipSlotEnum.of(request.getEquipSlot());

        PetEquipForge forge = getOrCreateForge(pet, slot);
        int currentLevel = forge.getEquipLevel() == null ? 1 : forge.getEquipLevel();

        if (currentLevel >= MAX_EQUIP_LEVEL) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "装备已达最高等级");
        }

        // 升级消耗积分 = 基础50 + 当前等级 * 20
        int cost = BASE_UPGRADE_COST + currentLevel * UPGRADE_COST_FACTOR;
        userPointsService.deductPoints(userId, cost,
                PointsRecordSourceEnum.OTHER.getValue(),
                String.valueOf(request.getPetId()),
                "宠物装备升级-" + slot.getLabel() + "-Lv" + currentLevel);

        // 升级成功概率随等级递减：基础80%，每级降低5%，最低1%
        int successRate = Math.max(1, 80 - currentLevel * 5);
        boolean success = new Random().nextInt(100) < successRate;

        if (success) {
            forge.setEquipLevel(currentLevel + 1);
            updateById(forge);
            log.info("宠物[{}]装备[{}]升级成功 {} -> {}", request.getPetId(), slot.getLabel(),
                    currentLevel, currentLevel + 1);
        } else {
            log.info("宠物[{}]装备[{}]升级失败，当前等级 {}", request.getPetId(), slot.getLabel(), currentLevel);
        }

        return success;
    }

    // ==================== 私有方法 ====================

    /**
     * 校验宠物归属并返回宠物实体
     */
    private FishPet getPetAndCheckOwner(Long petId, Long userId) {
        FishPet pet = fishPetService.getById(petId);
        if (pet == null || pet.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "宠物不存在");
        }
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作他人宠物");
        }
        return pet;
    }

    /**
     * 查询装备记录，不存在则初始化
     */
    private PetEquipForge getOrCreateForge(FishPet pet, EquipSlotEnum slot) {
        PetEquipForge forge = lambdaQuery()
                .eq(PetEquipForge::getPetId, pet.getPetId())
                .eq(PetEquipForge::getEquipSlot, slot.getValue())
                .one();
        if (forge == null) {
            forge = new PetEquipForge();
            forge.setPetId(pet.getPetId());
            forge.setEquipSlot(slot.getValue());
            forge.setEquipLevel(0);
            save(forge);
        }
        return forge;
    }

    /**
     * 随机生成一条词条（按等级权重随机，数值在对应等级区间内随机）
     */
    private EquipEntry randomEntry() {
        Random rng = new Random();
        EntryAttrEnum attr = ALL_ATTRS[rng.nextInt(ALL_ATTRS.length)];
        EntryGradeEnum grade = randomGrade();
        double value = randomValueInRange(attr, grade.getLevel(), rng);
        EquipEntry entry = new EquipEntry();
        entry.setAttr(attr.getValue());
        entry.setGrade(grade.getLevel());
        entry.setValue(value);
        entry.setLocked(false);
        return entry;
    }

    /**
     * 在属性对应等级区间内随机取值
     * 百分比属性保留两位小数，整数属性取整
     */
    private double randomValueInRange(EntryAttrEnum attr, int gradeLevel, Random rng) {
        double[] range = attr.getRangeByGrade(gradeLevel);
        double min = range[0];
        double max = range[1];
        double raw = min + rng.nextDouble() * (max - min);
        if (attr.isPercentage()) {
            // 百分比保留两位小数
            return Math.round(raw * 100.0) / 100.0;
        } else {
            // 整数属性取整
            return Math.round(raw);
        }
    }

    /**
     * 按权重随机词条等级
     * WHITE=50% BLUE=25% PURPLE=15% GOLD=8% RED=2%
     */
    private EntryGradeEnum randomGrade() {
        int roll = new Random().nextInt(100);
        int cumulative = 0;
        for (EntryGradeEnum grade : EntryGradeEnum.values()) {
            cumulative += grade.getWeight();
            if (roll < cumulative) return grade;
        }
        return EntryGradeEnum.WHITE;
    }

    private PetEquipForgeVO toVO(PetEquipForge forge) {
        PetEquipForgeVO vo = new PetEquipForgeVO();
        BeanUtils.copyProperties(forge, vo);
        EquipSlotEnum slot = EquipSlotEnum.of(forge.getEquipSlot());
        vo.setEquipSlotName(slot.getLabel());
        return vo;
    }
}
