package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.mapper.farm.FarmUserBuffMapper;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.dto.farm.FarmBuffVO;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.entity.farm.FarmUserBuff;
import com.cong.fishisland.model.enums.farm.FarmBuffTypeEnum;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.model.enums.farm.FarmLandStatusEnum;
import com.cong.fishisland.model.enums.farm.FarmYesNoEnum;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.FarmAchievementService;
import com.cong.fishisland.service.FarmCollectionGradeService;
import com.cong.fishisland.service.FarmUserBuffService;
import com.cong.fishisland.service.UserPointsService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 农场 buff 道具服务实现。
 * <p>依赖仅使用 Mapper（FarmLandMapper/FarmPlantRecordMapper/FarmUserMapper），
 * 避免 FarmLandServiceImpl ↔ 本服务互相注入产生循环依赖。</p>
 */
@Slf4j
@Service
public class FarmUserBuffServiceImpl extends ServiceImpl<FarmUserBuffMapper, FarmUserBuff>
        implements FarmUserBuffService {

    @Resource
    private FarmLandMapper farmLandMapper;

    @Resource
    private FarmPlantRecordMapper plantRecordMapper;

    @Resource
    private FarmUserMapper farmUserMapper;

    @Resource
    private FarmCollectionGradeService collectionGradeService;

    @Resource
    private FarmAchievementService achievementService;

    @Resource
    private UserPointsService userPointsService;

    @Override
    public List<FarmBuffVO> getMyBuffs(Long userId) {
        long unlockedLandCount = countUnlockedLands(userId);
        FarmUser farmUser = farmUserMapper.selectById(userId);
        int farmLevel = farmUser != null && farmUser.getLevel() != null ? farmUser.getLevel() : 1;

        List<FarmBuffVO> result = new ArrayList<>(FarmBuffTypeEnum.values().length);
        for (FarmBuffTypeEnum type : FarmBuffTypeEnum.values()) {
            FarmUserBuff buff = getOne(new LambdaQueryWrapper<FarmUserBuff>()
                    .eq(FarmUserBuff::getUserId, userId)
                    .eq(FarmUserBuff::getBuffType, type.getType()));
            boolean owned = buff != null;
            int level = owned ? buff.getLevel() : 0;
            boolean full = owned && level >= type.getMaxLevel();
            boolean unlockQualified = unlockedLandCount >= type.getRequiredLandCount()
                    && farmLevel >= type.getRequiredFarmLevel();

            Integer upgradeCost = null;
            Integer threshold = null;
            Boolean thresholdSatisfied = null;
            String nextEffectDesc = null;
            if (owned && !full) {
                int targetLevel = level + 1;
                upgradeCost = FarmConstants.BUFF_UPGRADE_COST[targetLevel];
                threshold = targetLevel;
                thresholdSatisfied = collectionGradeService.countEntriesWithGradeAtLeast(userId, targetLevel) >= 1;
                nextEffectDesc = type.effectDescription(targetLevel);
            }

            result.add(new FarmBuffVO(
                    type.getType(),
                    type.getName(),
                    owned,
                    level,
                    type.getMaxLevel(),
                    owned ? type.effectDescription(level) : null,
                    nextEffectDesc,
                    unlockQualified,
                    buildUnlockDesc(type),
                    type.getPurchaseCost(),
                    upgradeCost,
                    threshold,
                    thresholdSatisfied));
        }
        return result;
    }

    @Override
    public int getBuffPercent(Long userId, FarmBuffTypeEnum type) {
        if (userId == null || type == null) {
            return 0;
        }
        FarmUserBuff buff = getOne(new LambdaQueryWrapper<FarmUserBuff>()
                .eq(FarmUserBuff::getUserId, userId)
                .eq(FarmUserBuff::getBuffType, type.getType()));
        return buff == null ? 0 : type.effectAt(buff.getLevel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmUserBuff purchase(Integer buffType) {
        FarmBuffTypeEnum type = validateType(buffType);
        Long userId = StpUtil.getLoginIdAsLong();

        FarmUserBuff existing = getOne(new LambdaQueryWrapper<FarmUserBuff>()
                .eq(FarmUserBuff::getUserId, userId)
                .eq(FarmUserBuff::getBuffType, type.getType()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已拥有该道具，不可重复购买");
        }
        checkUnlock(userId, type);

        int cost = type.getPurchaseCost();
        if (cost > 0) {
            userPointsService.checkAvailablePoints(userId, cost);
            userPointsService.deductPoints(userId, cost,
                    PointsRecordSourceEnum.FARM_BUFF_BUY.getValue(),
                    String.valueOf(type.getType()),
                    "购买农场buff-" + type.getName());
        }

        LocalDateTime now = LocalDateTime.now();
        FarmUserBuff buff = new FarmUserBuff();
        buff.setUserId(userId);
        buff.setBuffType(type.getType());
        buff.setLevel(1);
        buff.setUnlockTime(now);
        buff.setUpdateTime(now);
        save(buff);

        // 生长加速：购买（0 → Lv1）同样对生长中作物实时生效
        if (type == FarmBuffTypeEnum.GROWTH_ACCEL) {
            rescaleGrowingHarvestTimes(userId, 0, type.effectAt(1));
        }

        achievementService.onProgress(userId,
                com.cong.fishisland.model.enums.farm.FarmAchievementTypeEnum.BUFF_PURCHASE,
                String.valueOf(type.getType()), 1);
        log.info("buff购买 userId={}, type={}, cost={}", userId, type.getType(), cost);
        return buff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmUserBuff upgrade(Integer buffType) {
        FarmBuffTypeEnum type = validateType(buffType);
        Long userId = StpUtil.getLoginIdAsLong();

        FarmUserBuff buff = getOne(new LambdaQueryWrapper<FarmUserBuff>()
                .eq(FarmUserBuff::getUserId, userId)
                .eq(FarmUserBuff::getBuffType, type.getType()));
        if (buff == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "尚未拥有该道具，请先购买");
        }
        int oldLevel = buff.getLevel();
        if (oldLevel >= type.getMaxLevel()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "道具已满级");
        }
        int targetLevel = oldLevel + 1;

        // 图鉴门槛：拥有品级 ≥ 目标等级 的图鉴记录（不消耗）
        long gradeEntries = collectionGradeService.countEntriesWithGradeAtLeast(userId, targetLevel);
        if (gradeEntries <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "图鉴门槛未达成：需先解锁任意品级 ≥ " + targetLevel + " 的作物图鉴");
        }

        // 积分消耗
        int cost = targetLevel < FarmConstants.BUFF_UPGRADE_COST.length
                ? FarmConstants.BUFF_UPGRADE_COST[targetLevel] : 0;
        if (cost > 0) {
            userPointsService.checkAvailablePoints(userId, cost);
            userPointsService.deductPoints(userId, cost,
                    PointsRecordSourceEnum.FARM_BUFF_UPGRADE.getValue(),
                    String.valueOf(type.getType()),
                    "升级农场buff-" + type.getName() + "至Lv" + targetLevel);
        }

        buff.setLevel(targetLevel);
        buff.setUpdateTime(LocalDateTime.now());
        updateById(buff);

        // 一次性里程碑奖励
        int reward = targetLevel < FarmConstants.BUFF_LEVEL_UP_REWARD.length
                ? FarmConstants.BUFF_LEVEL_UP_REWARD[targetLevel] : 0;
        if (reward > 0) {
            userPointsService.updateUsedPoints(userId, -reward,
                    PointsRecordSourceEnum.FARM_BUFF_LEVEL_REWARD.getValue(),
                    String.valueOf(type.getType()),
                    "buff升级里程碑奖励-" + type.getName() + "Lv" + targetLevel);
        }

        // 加速道具：升级对生长中作物实时生效（批量重算剩余成熟时间）
        if (type == FarmBuffTypeEnum.GROWTH_ACCEL) {
            rescaleGrowingHarvestTimes(userId, type.effectAt(oldLevel), type.effectAt(targetLevel));
        }

        achievementService.onProgress(userId,
                com.cong.fishisland.model.enums.farm.FarmAchievementTypeEnum.BUFF_UPGRADE,
                String.valueOf(type.getType()), targetLevel);
        log.info("buff升级 userId={}, type={}, Lv{}→Lv{}", userId, type.getType(), oldLevel, targetLevel);
        return buff;
    }

    /**
     * 加速等级变化后，按新旧折扣比批量重算所有生长中作物的剩余成熟时间：
     * 新剩余 = 旧剩余 × (100 - 新折扣) / (100 - 旧折扣)。等级只升不降，无需处理变长场景。
     */
    private void rescaleGrowingHarvestTimes(Long userId, int oldPercent, int newPercent) {
        if (oldPercent == newPercent) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<FarmLand> growing = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, userId)
                .eq(FarmLand::getLocked, FarmYesNoEnum.NO.getValue())
                .in(FarmLand::getStatus, FarmLandStatusEnum.PLANTING.getValue(),
                        FarmLandStatusEnum.MATURE.getValue())
                .gt(FarmLand::getHarvestTime, now));
        if (growing.isEmpty()) {
            return;
        }
        double ratio = (100.0 - newPercent) / (100.0 - oldPercent);
        for (FarmLand land : growing) {
            long remainingMillis = Duration.between(now, land.getHarvestTime()).toMillis();
            long newRemaining = Math.max(0, Math.round(remainingMillis * ratio));
            LocalDateTime newHarvestTime = now.plus(newRemaining, java.time.temporal.ChronoUnit.MILLIS);

            FarmLand landUpdate = new FarmLand();
            landUpdate.setId(land.getId());
            landUpdate.setHarvestTime(newHarvestTime);
            landUpdate.setUpdateTime(now);
            farmLandMapper.updateById(landUpdate);

            // 同步种植记录的预计收获时间（取该地块最新一条未收获记录）
            FarmPlantRecord record = plantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                    .eq(FarmPlantRecord::getLandId, land.getId())
                    .eq(FarmPlantRecord::getHarvested, FarmYesNoEnum.NO.getValue())
                    .orderByDesc(FarmPlantRecord::getId)
                    .last("LIMIT 1"));
            if (record != null) {
                FarmPlantRecord recordUpdate = new FarmPlantRecord();
                recordUpdate.setId(record.getId());
                recordUpdate.setHarvestTime(newHarvestTime);
                plantRecordMapper.updateById(recordUpdate);
            }
        }
        log.info("加速生效 userId={}, {}%→{}%, 重算{}块生长中地块", userId, oldPercent, newPercent, growing.size());
    }

    private void checkUnlock(Long userId, FarmBuffTypeEnum type) {
        long unlockedLandCount = countUnlockedLands(userId);
        if (unlockedLandCount < type.getRequiredLandCount()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "解锁" + type.getName() + "需要拥有" + type.getRequiredLandCount() + "块已解锁农田");
        }
        FarmUser farmUser = farmUserMapper.selectById(userId);
        int farmLevel = farmUser != null && farmUser.getLevel() != null ? farmUser.getLevel() : 1;
        if (farmLevel < type.getRequiredFarmLevel()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "解锁" + type.getName() + "需要农场等级达到" + type.getRequiredFarmLevel() + "级");
        }
    }

    private long countUnlockedLands(Long userId) {
        return farmLandMapper.selectCount(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, userId)
                .eq(FarmLand::getLocked, FarmYesNoEnum.NO.getValue()));
    }

    private String buildUnlockDesc(FarmBuffTypeEnum type) {
        StringBuilder desc = new StringBuilder("解锁 ");
        desc.append(type.getRequiredLandCount()).append(" 块农田");
        if (type.getRequiredFarmLevel() > 0) {
            desc.append(" 且农场等级达到 ").append(type.getRequiredFarmLevel()).append(" 级");
        }
        desc.append("开启");
        return desc.toString();
    }

    private FarmBuffTypeEnum validateType(Integer buffType) {
        FarmBuffTypeEnum type = FarmBuffTypeEnum.fromType(buffType);
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "道具类型无效");
        }
        return type;
    }
}
