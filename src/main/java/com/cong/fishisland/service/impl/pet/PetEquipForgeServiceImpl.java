package com.cong.fishisland.service.impl.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.PetForgeConstant;
import com.cong.fishisland.mapper.pet.PetEquipForgeMapper;
import com.cong.fishisland.model.dto.pet.ForgeLockRequest;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.entity.pet.EquipEntry;
import com.cong.fishisland.model.entity.pet.FishPet;
import com.cong.fishisland.model.entity.pet.PetEquipForge;
import com.cong.fishisland.model.enums.pet.EntryAttrEnum;
import com.cong.fishisland.model.enums.pet.EntryGradeEnum;
import com.cong.fishisland.model.enums.pet.EquipSlotEnum;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.pet.PetEquipForgeDetailVO;
import com.cong.fishisland.model.vo.pet.PetEquipForgeVO;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
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
    public PetEquipForgeDetailVO getForgeDetail(Long petId, Integer equipSlot) {
        Long userId = StpUtil.getLoginIdAsLong();
        FishPet pet = getPetAndCheckOwner(petId, userId);

        EquipSlotEnum slot = EquipSlotEnum.of(equipSlot);
        PetEquipForge forge = getOrCreateForge(pet, slot);

        int currentLevel = forge.getEquipLevel() == null ? 0 : forge.getEquipLevel();
        boolean isMaxLevel = currentLevel >= MAX_EQUIP_LEVEL;

        PetEquipForgeDetailVO vo = new PetEquipForgeDetailVO();
        BeanUtils.copyProperties(forge, vo);
        vo.setEquipSlotName(slot.getLabel());
        vo.setMaxLevel(isMaxLevel);

        if (isMaxLevel) {
            vo.setNextUpgradeCost(0);
            vo.setSuccessRate(0);
        } else {
            // 升级消耗积分 = 基础50 + 当前等级 * 20
            vo.setNextUpgradeCost(BASE_UPGRADE_COST + currentLevel * UPGRADE_COST_FACTOR);
            // 升级成功概率：基础80%，每级降低5%，最低1%
            vo.setSuccessRate(Math.max(1, 80 - currentLevel * 5));
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetEquipForgeVO lockEntries(ForgeLockRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        FishPet pet = getPetAndCheckOwner(request.getPetId(), userId);

        EquipSlotEnum slot = EquipSlotEnum.of(request.getEquipSlot());
        List<Integer> toLock = request.getLockedEntries() == null
                ? Collections.emptyList() : request.getLockedEntries();

        PetEquipForge forge = getOrCreateForge(pet, slot);

        EquipEntry[] entries = {forge.getEntry1(), forge.getEntry2(), forge.getEntry3(), forge.getEntry4()};
        for (int i = 1; i <= 4; i++) {
            if (entries[i - 1] != null) {
                entries[i - 1].setLocked(tolock(i, toLock));
            }
        }

        forge.setEntry1(entries[0]);
        forge.setEntry2(entries[1]);
        forge.setEntry3(entries[2]);
        forge.setEntry4(entries[3]);
        updateById(forge);

        return toVO(forge);
    }

    private boolean tolock(int index, List<Integer> lockedList) {
        return lockedList.contains(index);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PetEquipForgeVO refreshEntries(ForgeRefreshRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        FishPet pet = getPetAndCheckOwner(request.getPetId(), userId);

        EquipSlotEnum slot = EquipSlotEnum.of(request.getEquipSlot());

        // 查询或初始化装备记录
        PetEquipForge forge = getOrCreateForge(pet, slot);

        EquipEntry[] current = {forge.getEntry1(), forge.getEntry2(), forge.getEntry3(), forge.getEntry4()};

        // 统计已锁定词条数，用于计算积分消耗
        int lockedCount = 0;
        for (EquipEntry entry : current) {
            if (entry != null && Boolean.TRUE.equals(entry.getLocked())) {
                lockedCount++;
            }
        }

        // 计算积分消耗：基础 100 + 每锁定一条 50
        int cost = BASE_REFRESH_COST + lockedCount * LOCK_EXTRA_COST;
        userPointsService.deductPoints(userId, cost,
                PointsRecordSourceEnum.OTHER.getValue(),
                String.valueOf(request.getPetId()),
                "宠物装备词条刷新-" + slot.getLabel());

        // 刷新未锁定的词条，锁定的词条保留
        for (int i = 0; i < 4; i++) {
            if (current[i] == null || !Boolean.TRUE.equals(current[i].getLocked())) {
                current[i] = randomEntry();
            }
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
     * 百分比属性：区间单位为 %，存库时除以 100 转为小数（如 1.02% → 0.0102），保留四位小数
     * 整数属性：直接取整
     */
    private double randomValueInRange(EntryAttrEnum attr, int gradeLevel, Random rng) {
        double[] range = attr.getRangeByGrade(gradeLevel);
        double min = range[0];
        double max = range[1];
        double raw = min + rng.nextDouble() * (max - min);
        if (attr.isPercentage()) {
            // 区间单位为 %，除以 100 转为小数后保留四位小数
            return Math.round(raw / 100.0 * 10000.0) / 10000.0;
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

    // ==================== 装备等级加成 ====================

    /**
     * 计算装备等级加成（非线性：BASE * level^SCALE）
     * 高等级时成长更快，满级(20级)加成约为1级的89倍
     */
    private static double calcLevelBonus(double base, int level) {
        if (level <= 0) return 0;
        return base * Math.pow(level, PetForgeConstant.LEVEL_SCALE);
    }

    /**
     * 将装备等级加成按槽位差异化叠加到统计VO
     * <ul>
     *   <li>武器(1)  → 攻击力</li>
     *   <li>手套(2)  → 防御力</li>
     *   <li>鞋子(3)  → 速度</li>
     *   <li>头盔(4)  → 最大生命值</li>
     *   <li>项链(5)  → 暴击率</li>
     *   <li>翅膀(6)  → 连击率</li>
     * </ul>
     */
    static void applyLevelBonusBySlot(int equipSlot, int level, PetEquipStatsVO stats) {
        if (level <= 0 || equipSlot <= 0) return;
        switch (equipSlot) {
            case 1: // 武器 → 攻击力
                stats.setTotalBaseAttack(stats.getTotalBaseAttack()
                        + (int) calcLevelBonus(PetForgeConstant.WEAPON_ATK_BASE, level));
                break;
            case 2: // 手套 → 防御力
                stats.setTotalBaseDefense(stats.getTotalBaseDefense()
                        + (int) calcLevelBonus(PetForgeConstant.GLOVES_DEF_BASE, level));
                break;
            case 3: // 鞋子 → 速度
                stats.setSpeed(stats.getSpeed() == null
                        ? (int) calcLevelBonus(PetForgeConstant.SHOES_SPEED_BASE, level)
                        : stats.getSpeed() + (int) calcLevelBonus(PetForgeConstant.SHOES_SPEED_BASE, level));
                break;
            case 4: // 头盔 → 最大生命值
                stats.setTotalBaseHp(stats.getTotalBaseHp()
                        + (int) calcLevelBonus(PetForgeConstant.HELMET_HP_BASE, level));
                break;
            case 5: // 项链 → 暴击率
                stats.setCritRate(stats.getCritRate()
                        + round4(calcLevelBonus(PetForgeConstant.NECKLACE_CRIT_BASE, level) / 100.0));
                break;
            case 6: // 翅膀 → 连击率
                stats.setComboRate(stats.getComboRate()
                        + round4(calcLevelBonus(PetForgeConstant.WINGS_COMBO_BASE, level) / 100.0));
                break;
            default:
                log.warn("未知装备槽位: {}", equipSlot);
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    @Override
    public PetEquipStatsVO getForgeStatsByPetId(Long petId) {
        PetEquipStatsVO stats = new PetEquipStatsVO();
        stats.setTotalBaseAttack(0);
        stats.setTotalBaseDefense(0);
        stats.setTotalBaseHp(0);
        stats.setSpeed(0);
        stats.setCritRate(0.0);
        stats.setComboRate(0.0);
        stats.setDodgeRate(0.0);
        stats.setBlockRate(0.0);
        stats.setLifesteal(0.0);
        stats.setCritResistance(0.0);
        stats.setComboResistance(0.0);
        stats.setDodgeResistance(0.0);
        stats.setBlockResistance(0.0);
        stats.setLifestealResistance(0.0);

        if (petId == null) {
            return stats;
        }

        List<PetEquipForge> forgeList = lambdaQuery()
                .eq(PetEquipForge::getPetId, petId)
                .list();

        for (PetEquipForge forge : forgeList) {
            // 1. 装备等级加成（按槽位差异化，非线性成长）
            int level = forge.getEquipLevel() == null ? 0 : forge.getEquipLevel();
            int slot  = forge.getEquipSlot()  == null ? 0 : forge.getEquipSlot();
            applyLevelBonusBySlot(slot, level, stats);

            // 2. 词条属性累加
            applyEntry(forge.getEntry1(), stats);
            applyEntry(forge.getEntry2(), stats);
            applyEntry(forge.getEntry3(), stats);
            applyEntry(forge.getEntry4(), stats);
        }

        return stats;
    }

    /**
     * 将单条词条的属性值累加到统计VO中
     */
    private void applyEntry(EquipEntry entry, PetEquipStatsVO stats) {
        if (entry == null || entry.getAttr() == null || entry.getValue() == null) {
            return;
        }
        double value = entry.getValue();
        switch (entry.getAttr()) {
            case "attack":
                stats.setTotalBaseAttack(stats.getTotalBaseAttack() + (int) value);
                break;
            case "maxHp":
                stats.setTotalBaseHp(stats.getTotalBaseHp() + (int) value);
                break;
            case "defense":
                stats.setTotalBaseDefense(stats.getTotalBaseDefense() + (int) value);
                break;
            case "speed":
                stats.setSpeed(stats.getSpeed() == null ? (int) value : stats.getSpeed() + (int) value);
                break;
            case "critRate":
                stats.setCritRate(stats.getCritRate() + value);
                break;
            case "comboRate":
                stats.setComboRate(stats.getComboRate() + value);
                break;
            case "dodgeRate":
                stats.setDodgeRate(stats.getDodgeRate() + value);
                break;
            case "blockRate":
                stats.setBlockRate(stats.getBlockRate() + value);
                break;
            case "lifesteal":
                stats.setLifesteal(stats.getLifesteal() + value);
                break;
            case "antiCrit":
                stats.setCritResistance(stats.getCritResistance() + value);
                break;
            case "antiCombo":
                stats.setComboResistance(stats.getComboResistance() + value);
                break;
            case "antiDodge":
                stats.setDodgeResistance(stats.getDodgeResistance() + value);
                break;
            case "antiBlock":
                stats.setBlockResistance(stats.getBlockResistance() + value);
                break;
            case "antiLifesteal":
                stats.setLifestealResistance(stats.getLifestealResistance() + value);
                break;
            default:
                log.warn("未知词条属性: {}", entry.getAttr());
                break;
        }
    }
}
