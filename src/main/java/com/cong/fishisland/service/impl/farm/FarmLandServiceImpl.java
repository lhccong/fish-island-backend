package com.cong.fishisland.service.impl.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.dto.farm.PlantItem;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.model.enums.farm.FarmLandStatusEnum;
import com.cong.fishisland.model.enums.farm.FarmYesNoEnum;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.service.FarmCollectionService;
import com.cong.fishisland.service.FarmCropService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmUserService;
import com.cong.fishisland.service.UserPointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmLandServiceImpl extends ServiceImpl<FarmLandMapper, FarmLand> implements FarmLandService {

    @Autowired
    private FarmCropMapper cropMapper;

    @Autowired
    private FarmPlantRecordMapper plantRecordMapper;

    @Autowired
    private FarmUserService farmUserService;

    @Autowired
    private FarmCollectionService collectionService;

    @Autowired
    private FarmCropService cropService;

    @Autowired
    private UserPointsService userPointsService;

    @Override
    public List<FarmLand> getLandsByUserId(Long userId) {
        List<FarmLand> farmLands = list(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, userId)
                .orderByAsc(FarmLand::getLandIndex));
        if (CollectionUtils.isEmpty(farmLands)) {
            initLands(userId);
            farmLands = list(new LambdaQueryWrapper<FarmLand>()
                    .eq(FarmLand::getUserId, userId)
                    .orderByAsc(FarmLand::getLandIndex));
        }
        return farmLands;
    }

    @Override
    public void initLands(Long userId) {
        long existingCount = count(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        if (existingCount < FarmConstants.LAND_TOTAL_COUNT) {
            List<FarmLand> lands = new ArrayList<>();
            for (int i = (int) existingCount + 1; i <= FarmConstants.LAND_TOTAL_COUNT; i++) {
                FarmLand land = new FarmLand();
                land.setUserId(userId);
                land.setLandIndex(i);
                land.setStatus(FarmLandStatusEnum.IDLE.getValue());
                land.setLocked(i > FarmConstants.LAND_DEFAULT_UNLOCKED_COUNT
                        ? FarmYesNoEnum.YES.getValue() : FarmYesNoEnum.NO.getValue());
                land.setCreateTime(now);
                land.setUpdateTime(now);
                lands.add(land);
            }
            saveBatch(lands);
        }
        // 同步默认解锁：序号 1-8 应为未锁定（兼容旧数据仅解锁 3 块的情况）
        lambdaUpdate()
                .eq(FarmLand::getUserId, userId)
                .le(FarmLand::getLandIndex, FarmConstants.LAND_DEFAULT_UNLOCKED_COUNT)
                .eq(FarmLand::getLocked, FarmYesNoEnum.YES.getValue())
                .set(FarmLand::getLocked, FarmYesNoEnum.NO.getValue())
                .set(FarmLand::getUpdateTime, now)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmLand unlockLand(Long landId) {
        if (landId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地块ID不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        FarmUser farmUser = farmUserService.getFarmUser(userId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "农场用户不存在");
        }

        FarmLand land = getById(landId);
        if (land == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "地块不存在");
        }
        if (!land.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该地块");
        }
        if (FarmYesNoEnum.isNo(land.getLocked())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块已解锁");
        }

        Integer landIndex = land.getLandIndex();
        if (landIndex == null || !FarmConstants.isLevelUnlockableLandIndex(landIndex)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该地块暂不支持解锁");
        }

        int requiredLevel = FarmConstants.unlockLevelForLandIndex(landIndex);
        int farmLevel = farmUser.getLevel() != null ? farmUser.getLevel() : 1;
        if (farmLevel < requiredLevel) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "农场等级不足，解锁第" + landIndex + "块地需要达到" + requiredLevel + "级");
        }

        // 顺序解锁：前一块必须已解锁
        if (landIndex > 1) {
            FarmLand prevLand = getOne(new LambdaQueryWrapper<FarmLand>()
                    .eq(FarmLand::getUserId, userId)
                    .eq(FarmLand::getLandIndex, landIndex - 1)
                    .last("LIMIT 1"));
            if (prevLand == null || FarmYesNoEnum.isYes(prevLand.getLocked())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "请先解锁第" + (landIndex - 1) + "块地");
            }
        }

        int unlockCost = FarmConstants.unlockCostForLandIndex(landIndex);
        if (unlockCost > 0) {
            userPointsService.checkAvailablePoints(userId, unlockCost);
            userPointsService.deductPoints(userId, unlockCost,
                    PointsRecordSourceEnum.FARM_LAND_UNLOCK.getValue(),
                    landId.toString(),
                    "农场解锁第" + landIndex + "块地");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean updated = lambdaUpdate()
                .eq(FarmLand::getId, landId)
                .eq(FarmLand::getUserId, userId)
                .eq(FarmLand::getLocked, FarmYesNoEnum.YES.getValue())
                .set(FarmLand::getLocked, FarmYesNoEnum.NO.getValue())
                .set(FarmLand::getUpdateTime, now)
                .update();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块状态已变化，无法解锁");
        }

        land.setLocked(FarmYesNoEnum.NO.getValue());
        land.setUpdateTime(now);
        return land;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FarmLand> plantBatch(List<PlantItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "种植列表不能为空");
        }
        if (items.size() > FarmConstants.LAND_TOTAL_COUNT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单次最多种植" + FarmConstants.LAND_TOTAL_COUNT + "块地");
        }

        Set<Long> landIdSet = new HashSet<>();
        for (PlantItem item : items) {
            if (item == null || item.getLandId() == null || item.getCropId() == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "种植项的地块ID与作物ID不能为空");
            }
            if (!landIdSet.add(item.getLandId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在重复的地块，无法重复种植");
            }
        }

        Long userId = StpUtil.getLoginIdAsLong();
        farmUserService.getFarmUser(userId);

        FarmUser farmUser = farmUserService.getById(userId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "农场用户不存在");
        }

        List<Long> landIds = items.stream().map(PlantItem::getLandId).collect(Collectors.toList());
        List<Long> cropIds = items.stream().map(PlantItem::getCropId).distinct().collect(Collectors.toList());

        Map<Long, FarmLand> landMap = listByIds(landIds).stream()
                .collect(Collectors.toMap(FarmLand::getId, Function.identity()));
        Map<Long, FarmCrop> cropMap = cropMapper.selectBatchIds(cropIds).stream()
                .collect(Collectors.toMap(FarmCrop::getId, Function.identity()));

        int totalSeedCost = 0;
        for (PlantItem item : items) {
            FarmLand land = landMap.get(item.getLandId());
            if (land == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "地块不存在");
            }
            if (!land.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该地块");
            }
            if (FarmYesNoEnum.isYes(land.getLocked())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块未解锁，无法种植");
            }
            if (!Integer.valueOf(FarmLandStatusEnum.IDLE.getValue()).equals(land.getStatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块未空闲，无法种植");
            }
            FarmCrop crop = cropMap.get(item.getCropId());
            if (crop == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作物不存在");
            }
            if (!cropService.isUnlocked(crop, farmUser.getLevel())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "农场等级不足，该作物未解锁");
            }
            totalSeedCost += crop.getPrice() != null ? crop.getPrice() : 0;
        }
        if (totalSeedCost > 0) {
            userPointsService.checkAvailablePoints(userId, totalSeedCost);
        }

        LocalDateTime now = LocalDateTime.now();
        List<FarmLand> updatedLands = new ArrayList<>(items.size());

        for (PlantItem item : items) {
            Long landId = item.getLandId();
            Long cropId = item.getCropId();

            FarmLand land = landMap.get(landId);
            FarmCrop crop = cropMap.get(cropId);

            int seedCost = crop.getPrice() != null ? crop.getPrice() : 0;
            if (seedCost > 0) {
                userPointsService.deductPoints(farmUser.getUserId(), seedCost,
                        PointsRecordSourceEnum.FARM_PLANT.getValue(),
                        cropId.toString(),
                        "农场种植购买种子-" + crop.getName());
            }

            LocalDateTime harvestTime = now.plusMinutes(crop.getGrowthTime());

            boolean updated = lambdaUpdate()
                    .eq(FarmLand::getId, landId)
                    .eq(FarmLand::getUserId, userId)
                    .eq(FarmLand::getStatus, FarmLandStatusEnum.IDLE.getValue())
                    .eq(FarmLand::getLocked, FarmYesNoEnum.NO.getValue())
                    .set(FarmLand::getStatus, FarmLandStatusEnum.PLANTING.getValue())
                    .set(FarmLand::getPlantedCropId, cropId)
                    .set(FarmLand::getPlantedTime, now)
                    .set(FarmLand::getHarvestTime, harvestTime)
                    .set(FarmLand::getUpdateTime, now)
                    .update();
            if (!updated) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块状态已变化，无法种植");
            }

            land.setStatus(FarmLandStatusEnum.PLANTING.getValue());
            land.setPlantedCropId(cropId);
            land.setPlantedTime(now);
            land.setHarvestTime(harvestTime);
            land.setUpdateTime(now);
            updatedLands.add(land);

            FarmPlantRecord record = new FarmPlantRecord();
            record.setUserId(userId);
            record.setLandId(landId);
            record.setCropId(cropId);
            record.setPlantedTime(now);
            record.setHarvestTime(harvestTime);
            record.setPlantedPointsReward(crop.getCoin());
            record.setCreateTime(now);
            plantRecordMapper.insert(record);
        }

        return updatedLands;
    }

    /**
     * 批量收获当前登录用户指定地块上的作物。
     * <p>可收获条件：状态为「已成熟」，或仍为「种植中」但已到达预计收获时间。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FarmLand> harvestBatch(List<Long> landIds) {
        if (CollectionUtils.isEmpty(landIds)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收获列表不能为空");
        }
        if (landIds.size() > FarmConstants.LAND_TOTAL_COUNT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单次最多收获" + FarmConstants.LAND_TOTAL_COUNT + "块地");
        }

        Set<Long> landIdSet = new HashSet<>();
        for (Long landId : landIds) {
            if (landId == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "地块ID不能为空");
            }
            if (!landIdSet.add(landId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "存在重复的地块，无法重复收获");
            }
        }

        long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, FarmLand> landMap = listByIds(landIds).stream()
                .collect(Collectors.toMap(FarmLand::getId, Function.identity()));

        Map<Long, FarmPlantRecord> recordMap = plantRecordMapper.selectList(
                        new LambdaQueryWrapper<FarmPlantRecord>()
                                .in(FarmPlantRecord::getLandId, landIds)
                                .eq(FarmPlantRecord::getHarvested, FarmYesNoEnum.NO.getValue()))
                .stream()
                .collect(Collectors.toMap(FarmPlantRecord::getLandId, Function.identity(), (a, b) -> a));

        List<Long> cropIds = landMap.values().stream()
                .map(FarmLand::getPlantedCropId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, FarmCrop> cropMap = cropIds.isEmpty()
                ? Collections.emptyMap()
                : cropMapper.selectBatchIds(cropIds).stream()
                .collect(Collectors.toMap(FarmCrop::getId, Function.identity()));

        List<FarmLand> updatedLands = new ArrayList<>(landIds.size());

        for (Long landId : landIds) {
            FarmLand land = landMap.get(landId);
            if (land == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "地块不存在");
            }
            if (!land.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该地块");
            }
            if (!FarmLandStatusEnum.isPlanted(land.getStatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块空闲，无可收获");
            }

            boolean mature = Integer.valueOf(FarmLandStatusEnum.MATURE.getValue()).equals(land.getStatus());
            boolean timeReached = land.getHarvestTime() != null && !land.getHarvestTime().isAfter(now);
            if (!mature && !timeReached) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物尚未成熟，无法收获");
            }

            FarmPlantRecord record = recordMap.get(landId);
            if (record == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "种植记录不存在，无法收获");
            }

            int marked = plantRecordMapper.markHarvestedIfNot(record.getId(), now);
            if (marked <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物已被收获");
            }

            boolean landUpdated = lambdaUpdate()
                    .eq(FarmLand::getId, landId)
                    .eq(FarmLand::getUserId, userId)
                    .in(FarmLand::getStatus,
                            FarmLandStatusEnum.PLANTING.getValue(),
                            FarmLandStatusEnum.MATURE.getValue())
                    .set(FarmLand::getStatus, FarmLandStatusEnum.IDLE.getValue())
                    .set(FarmLand::getPlantedCropId, null)
                    .set(FarmLand::getPlantedTime, null)
                    .set(FarmLand::getHarvestTime, null)
                    .set(FarmLand::getUpdateTime, now)
                    .update();
            if (!landUpdated) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "地块状态已变化，无法收获");
            }

            FarmCrop crop = cropMap.get(land.getPlantedCropId());
            if (crop != null) {
                int actualReward = calcHarvestPointsReward(crop, record);
                if (actualReward > 0) {
                    userPointsService.updateUsedPoints(userId, -actualReward,
                            PointsRecordSourceEnum.FARM_HARVEST.getValue(),
                            record.getId().toString(),
                            buildHarvestPointsDescription(crop, record, actualReward));
                }

                int exp = crop.getExperience() != null ? crop.getExperience() : 0;
                if (exp > 0) {
                    farmUserService.addExperience(userId, exp);
                }
                farmUserService.incrementTotalHarvest(userId);
                collectionService.updateCollection(userId, crop.getId());
            }

            land.setStatus(FarmLandStatusEnum.IDLE.getValue());
            land.setPlantedCropId(null);
            land.setPlantedTime(null);
            land.setHarvestTime(null);
            land.setUpdateTime(now);
            updatedLands.add(land);
        }

        return updatedLands;
    }

    /**
     * 计算收获可得积分：种植预期奖励减去已被偷积分，且不低于种子价格 + 1（与偷菜上限逻辑一致）。
     */
    private static int calcHarvestPointsReward(FarmCrop crop, FarmPlantRecord record) {
        int baseReward = record.getPlantedPointsReward() != null
                ? record.getPlantedPointsReward()
                : (crop.getCoin() != null ? crop.getCoin() : 0);
        int stolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
        int minReward = FarmConstants.minHarvestPoints(crop.getPrice());
        return Math.max(minReward, baseReward - stolenPoints);
    }

    private static String buildHarvestPointsDescription(FarmCrop crop, FarmPlantRecord record, int actualReward) {
        int stolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
        String cropName = crop.getName() != null ? crop.getName() : "作物";
        if (stolenPoints > 0) {
            return String.format("农场收获-%s（实得%d积分，已被偷%d积分）", cropName, actualReward, stolenPoints);
        }
        return "农场收获-" + cropName;
    }

    @Override
    public LandDTO toDTO(FarmLand land) {
        if (land == null) {
            return null;
        }
        LandDTO dto = new LandDTO();
        dto.setId(land.getId());
        dto.setLandIndex(land.getLandIndex());
        dto.setStatus(FarmLandStatusEnum.resolveDisplayStatus(
                land.getStatus(), land.getHarvestTime(), LocalDateTime.now()));
        dto.setPlantedCropId(land.getPlantedCropId());
        dto.setPlantedTime(land.getPlantedTime());
        dto.setHarvestTime(land.getHarvestTime());
        dto.setLocked(land.getLocked());
        if (land.getLandIndex() != null && FarmConstants.isLevelUnlockableLandIndex(land.getLandIndex())) {
            dto.setUnlockLevel(FarmConstants.unlockLevelForLandIndex(land.getLandIndex()));
            dto.setUnlockCost(FarmConstants.unlockCostForLandIndex(land.getLandIndex()));
        }

        if (land.getPlantedCropId() != null) {
            FarmCrop crop = cropMapper.selectById(land.getPlantedCropId());
            if (crop != null) {
                dto.setCropName(crop.getName());
            }
        }
        return dto;
    }

    @Override
    public List<LandDTO> toDTOList(List<FarmLand> lands) {
        if (lands == null || lands.isEmpty()) {
            return Collections.emptyList();
        }
        return lands.stream().map(this::toDTO).collect(Collectors.toList());
    }

}
