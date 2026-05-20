package com.cong.fishisland.service.impl.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.service.FarmCollectionService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmUserService;
import com.cong.fishisland.service.UserPointsRecordService;
import com.cong.fishisland.service.UserPointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.FARM_HARVEST;
import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.FARM_PLANT;

@Service
public class FarmLandServiceImpl extends ServiceImpl<FarmLandMapper, FarmLand> implements FarmLandService {

    @Autowired
    private FarmCropMapper cropMapper;

    @Autowired
    private FarmPlantRecordMapper plantRecordMapper;

    @Autowired
    private UserPointsService userPointsService;

    @Autowired
    private UserPointsRecordService userPointsRecordService;

    @Autowired
    private FarmUserService farmUserService;

    @Autowired
    private FarmCollectionService collectionService;

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
    public FarmLand getLand(Long landId) {
        return getById(landId);
    }

    @Override
    public void initLands(Long userId) {
        long existingCount = count(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, userId));
        if (existingCount >= 9) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<FarmLand> lands = new ArrayList<>();
        for (int i = (int) existingCount + 1; i <= 9; i++) {
            FarmLand land = new FarmLand();
            land.setUserId(userId);
            land.setLandIndex(i);
            land.setStatus(0);
            land.setLocked(i > 3 ? 1 : 0);
            land.setCreatedAt(now);
            land.setUpdatedAt(now);
            lands.add(land);
        }
        saveBatch(lands);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmLand plant(Long userId, Long landId, Long cropId) {
        FarmLand land = getById(landId);
        if (land == null || !land.getUserId().equals(userId)) {
            return null;
        }
        if (land.getStatus() != 0) {
            return null;
        }

        FarmCrop crop = cropMapper.selectById(cropId);
        if (crop == null) {
            return null;
        }

        Long systemUserId = StpUtil.getLoginIdAsLong();

        if (crop.getPrice() != null && crop.getPrice() > 0) {
            userPointsService.checkAvailablePoints(systemUserId, crop.getPrice());
            userPointsService.deductPoints(systemUserId, crop.getPrice(), FARM_PLANT.getValue(),
                    "crop_" + cropId, "购买作物种子: " + crop.getName());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime harvestTime = now.plusMinutes(crop.getGrowthTime());

        land.setStatus(1);
        land.setPlantedCropId(cropId);
        land.setPlantedTime(now);
        land.setHarvestTime(harvestTime);
        land.setUpdatedAt(now);
        updateById(land);

        FarmPlantRecord record = new FarmPlantRecord();
        record.setUserId(userId);
        record.setLandId(landId);
        record.setCropId(cropId);
        record.setPlantedTime(now);
        record.setHarvestTime(harvestTime);
        record.setPlantedPointsReward(crop.getCoin());
        record.setCreatedAt(now);
        plantRecordMapper.insert(record);

        return land;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmLand harvest(Long userId, Long landId) {
        FarmLand land = getById(landId);
        if (land == null || !land.getUserId().equals(userId)) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        if (land.getStatus() != 2 && land.getHarvestTime().isAfter(now)) {
            return null;
        }

        FarmCrop crop = cropMapper.selectById(land.getPlantedCropId());
        FarmPlantRecord record = plantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                .eq(FarmPlantRecord::getLandId, landId)
                .eq(FarmPlantRecord::getHarvested, 0)
                .last("LIMIT 1"));

        if (crop != null && record != null) {
            int baseReward = record.getPlantedPointsReward() != null ? record.getPlantedPointsReward() : crop.getCoin();
            int stolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
            int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
            int actualReward = Math.max(baseReward - stolenPoints, minReward);

            if (actualReward > 0) {
                Long systemUserId = StpUtil.getLoginIdAsLong();
                UserPoints userPoints = userPointsService.getById(systemUserId);
                int beforePoints = userPoints.getPoints();
                int afterPoints = beforePoints + actualReward;
                int usedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();

                userPointsService.updatePoints(systemUserId, actualReward, false);

                userPointsRecordService.addPointsIncreaseRecord(systemUserId, actualReward, FARM_HARVEST.getValue(),
                        "收获作物: " + crop.getName() + (stolenPoints > 0 ? " (被偷损失" + stolenPoints + "积分)" : ""),
                        beforePoints, afterPoints, usedPoints, usedPoints);
            }

            farmUserService.addExperience(userId, crop.getExperience());
            farmUserService.incrementTotalHarvest(userId);
            collectionService.updateCollection(userId, crop.getId());
        }

        if (record != null) {
            record.setHarvested(1);
            record.setHarvestedTime(now);
            plantRecordMapper.updateById(record);
        }

        land.setStatus(0);
        land.setPlantedCropId(null);
        land.setPlantedTime(null);
        land.setHarvestTime(null);
        land.setUpdatedAt(now);
        updateById(land);

        return land;
    }

    @Override
    public void updateLandStatus() {
        LocalDateTime now = LocalDateTime.now();
        baseMapper.updateMatureLands(now);
    }

}
