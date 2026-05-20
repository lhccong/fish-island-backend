package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.model.enums.farm.FarmLandStatusEnum;
import com.cong.fishisland.model.enums.farm.FarmYesNoEnum;
import com.cong.fishisland.service.FarmCollectionService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        if (existingCount >= FarmConstants.LAND_TOTAL_COUNT) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmLand plant(Long userId, Long landId, Long cropId) {

        FarmLand land = getById(landId);
        if (land == null || !land.getUserId().equals(userId)) {
            return null;
        }
        if (!Integer.valueOf(FarmLandStatusEnum.IDLE.getValue()).equals(land.getStatus())) {
            return null;
        }

        FarmCrop crop = cropMapper.selectById(cropId);
        if (crop == null) {
            return null;
        }

        //更新土地信息
        // TODO: 种植时扣减种子积分（校验余额并记录 FARM_PLANT 流水）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime harvestTime = now.plusMinutes(crop.getGrowthTime());

        land.setStatus(FarmLandStatusEnum.PLANTING.getValue());
        land.setPlantedCropId(cropId);
        land.setPlantedTime(now);
        land.setHarvestTime(harvestTime);
        land.setUpdateTime(now);
        updateById(land);

        //添加种植记录
        FarmPlantRecord record = new FarmPlantRecord();
        record.setUserId(userId);
        record.setLandId(landId);
        record.setCropId(cropId);
        record.setPlantedTime(now);
        record.setHarvestTime(harvestTime);
        record.setPlantedPointsReward(crop.getCoin());
        record.setCreateTime(now);
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
        if (!Integer.valueOf(FarmLandStatusEnum.MATURE.getValue()).equals(land.getStatus())
                && land.getHarvestTime().isAfter(now)) {
            return null;
        }

        FarmCrop crop = cropMapper.selectById(land.getPlantedCropId());
        FarmPlantRecord record = plantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                .eq(FarmPlantRecord::getLandId, landId)
                .eq(FarmPlantRecord::getHarvested, FarmYesNoEnum.NO.getValue())
                .last("LIMIT 1"));

        if (crop != null && record != null) {
            // TODO: 收获时发放积分（按被偷损失计算实际奖励并更新用户积分、记录 FARM_HARVEST 流水）

            farmUserService.addExperience(userId, crop.getExperience());
            farmUserService.incrementTotalHarvest(userId);
            collectionService.updateCollection(userId, crop.getId());
        }

        if (record != null) {
            record.setHarvested(FarmYesNoEnum.YES.getValue());
            record.setHarvestedTime(now);
            plantRecordMapper.updateById(record);
        }

        land.setStatus(FarmLandStatusEnum.IDLE.getValue());
        land.setPlantedCropId(null);
        land.setPlantedTime(null);
        land.setHarvestTime(null);
        land.setUpdateTime(now);
        updateById(land);

        return land;
    }

    @Override
    public LandDTO toDTO(FarmLand land) {
        if (land == null) {
            return null;
        }
        LandDTO dto = new LandDTO();
        dto.setId(land.getId());
        dto.setLandIndex(land.getLandIndex());
        dto.setStatus(land.getStatus());
        dto.setPlantedCropId(land.getPlantedCropId());
        dto.setPlantedTime(land.getPlantedTime());
        dto.setHarvestTime(land.getHarvestTime());
        dto.setLocked(land.getLocked());

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
