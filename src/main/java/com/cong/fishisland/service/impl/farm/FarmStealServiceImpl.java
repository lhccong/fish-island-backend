package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.mapper.farm.FarmStealRecordMapper;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.model.enums.farm.FarmLandStatusEnum;
import com.cong.fishisland.model.enums.farm.FarmTaskTypeEnum;
import com.cong.fishisland.model.enums.farm.FarmYesNoEnum;
import com.cong.fishisland.service.*;
import com.cong.fishisland.service.event.EventRemindHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmStealServiceImpl implements FarmStealService {

    @Autowired
    private FarmStealRecordMapper stealRecordMapper;

    @Autowired
    private FarmPlantRecordMapper plantRecordMapper;

    @Autowired
    private FarmLandMapper landMapper;

    @Autowired
    private FarmCropMapper cropMapper;

    @Autowired
    private FarmRankingService rankingService;

    @Autowired
    private FarmTaskService farmTaskService;

    @Autowired
    private FarmUserService farmUserService;

    @Autowired
    private UserFollowService userFollowService;

    @Autowired
    private EventRemindHandler eventRemindHandler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmStealRecord steal(Long stealerId, Long landId) {
        FarmLand land = landMapper.selectById(landId);
        if (land == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "地块不存在");
        }
        if (!FarmLandStatusEnum.isPlanted(land.getStatus()) || land.getPlantedCropId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该地块没有可偷的作物");
        }

        FarmPlantRecord plantRecord = getCurrentPlantRecord(landId);
        if (plantRecord == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "种植记录不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (FarmYesNoEnum.isYes(plantRecord.getHarvested())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物已被收获");
        }
        boolean mature = Integer.valueOf(FarmLandStatusEnum.MATURE.getValue()).equals(land.getStatus());
        boolean timeReached = land.getHarvestTime() != null && !land.getHarvestTime().isAfter(now);
        if (!mature && !timeReached) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物尚未成熟");
        }

        if (hasStolenLandCrop(stealerId, landId, plantRecord.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已偷过该地块，无法重复偷取");
        }

        FarmCrop crop = cropMapper.selectById(plantRecord.getCropId());
        if (crop == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物信息不存在");
        }

        Long ownerId = land.getUserId();
        if (stealerId.equals(ownerId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能偷自己的作物");
        }

        if (!validateFriend(stealerId, ownerId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能偷互相关注用户的作物");
        }

        int baseReward = plantRecord.getPlantedPointsReward() != null ? plantRecord.getPlantedPointsReward() : crop.getCoin();
        int currentStolenPoints = plantRecord.getStolenPoints() != null ? plantRecord.getStolenPoints() : 0;
        int stealPoints = calcStealPoints(crop, baseReward, currentStolenPoints);

        FarmStealRecord stealRecord = new FarmStealRecord();
        stealRecord.setStealerId(stealerId);
        stealRecord.setOwnerId(ownerId);
        stealRecord.setLandId(landId);
        stealRecord.setPlantRecordId(plantRecord.getId());
        stealRecord.setCropId(crop.getId());
        stealRecord.setStolenTime(LocalDateTime.now());
        stealRecord.setCoinGained(stealPoints);
        stealRecordMapper.insert(stealRecord);

        plantRecord.setStolenCount(plantRecord.getStolenCount() + 1);
        plantRecord.setStolenPoints(currentStolenPoints + stealPoints);
        plantRecordMapper.updateById(plantRecord);

        // TODO: 偷菜成功后为偷取者发放积分（更新用户积分并记录 FARM_STEAL 流水）

        rankingService.updateStealCountRanking(stealerId);

        farmUserService.incrementTotalSteal(stealerId);
        farmUserService.incrementTotalDefense(ownerId);

        farmTaskService.updateTaskProgress(FarmTaskTypeEnum.STEAL);

        eventRemindHandler.handleFarmSteal(
                stealRecord.getId(),
                landId,
                stealerId,
                ownerId,
                crop.getName(),
                stealPoints);

        return stealRecord;
    }

    /**
     * 每次偷菜固定 1 积分，且不超过该作物剩余可偷额度。
     */
    static int calcStealPoints(FarmCrop crop, int baseReward, int currentStolenPoints) {
        int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
        int remainingStealable = (baseReward - minReward) - currentStolenPoints;

        if (remainingStealable <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该地块已无可偷积分");
        }

        return Math.min(FarmConstants.MAX_STEAL_POINTS_PER_ACTION, remainingStealable);
    }

    static int remainingStealablePoints(FarmCrop crop, FarmPlantRecord record) {
        int baseReward = record.getPlantedPointsReward() != null ? record.getPlantedPointsReward() : crop.getCoin();
        int currentStolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
        int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
        return (baseReward - minReward) - currentStolenPoints;
    }

    private boolean hasStolenLandCrop(Long stealerId, Long landId, Long plantRecordId) {
        return stealRecordMapper.selectCount(new LambdaQueryWrapper<FarmStealRecord>()
                .eq(FarmStealRecord::getStealerId, stealerId)
                .eq(FarmStealRecord::getPlantRecordId, plantRecordId)
                .eq(FarmStealRecord::getLandId, landId)) > 0
                || stealRecordMapper.selectCount(new LambdaQueryWrapper<FarmStealRecord>()
                .eq(FarmStealRecord::getStealerId, stealerId)
                .eq(FarmStealRecord::getPlantRecordId, plantRecordId)) > 0;
    }

    @Override
    public Set<Long> findStolenLandIdsForCurrentCrop(Long stealerId, Collection<Long> landIds) {
        if (stealerId == null || CollectionUtils.isEmpty(landIds)) {
            return Collections.emptySet();
        }
        Map<Long, FarmPlantRecord> latestByLand = loadLatestUnharvestedPlantRecords(new ArrayList<>(landIds));
        if (latestByLand.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> plantRecordIds = latestByLand.values().stream()
                .map(FarmPlantRecord::getId)
                .collect(Collectors.toList());
        List<FarmStealRecord> stealRecords = stealRecordMapper.selectList(new LambdaQueryWrapper<FarmStealRecord>()
                .eq(FarmStealRecord::getStealerId, stealerId)
                .in(FarmStealRecord::getPlantRecordId, plantRecordIds));
        Set<Long> stolenPlantRecordIds = stealRecords.stream()
                .map(FarmStealRecord::getPlantRecordId)
                .collect(Collectors.toSet());

        Set<Long> stolenLandIds = new HashSet<>();
        for (Map.Entry<Long, FarmPlantRecord> entry : latestByLand.entrySet()) {
            if (stolenPlantRecordIds.contains(entry.getValue().getId())) {
                stolenLandIds.add(entry.getKey());
            }
        }
        return stolenLandIds;
    }

    @Override
    public boolean canStealLand(Long stealerId, Long landId) {
        FarmLand land = landMapper.selectById(landId);
        if (land == null) {
            return false;
        }
        if (stealerId.equals(land.getUserId())) {
            return false;
        }
        if (!validateFriend(stealerId, land.getUserId())) {
            return false;
        }
        return batchCanStealLand(stealerId, Collections.singletonList(land))
                .getOrDefault(landId, false);
    }

    @Override
    public Map<Long, Boolean> batchCanStealLand(Long stealerId, List<FarmLand> lands) {
        Map<Long, Boolean> result = new HashMap<>();
        if (stealerId == null || CollectionUtils.isEmpty(lands)) {
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        List<FarmLand> candidateLands = new ArrayList<>();
        for (FarmLand land : lands) {
            result.put(land.getId(), false);
            if (stealerId.equals(land.getUserId())) {
                continue;
            }
            if (!FarmLandStatusEnum.isPlanted(land.getStatus()) || land.getPlantedCropId() == null) {
                continue;
            }
            boolean mature = Integer.valueOf(FarmLandStatusEnum.MATURE.getValue()).equals(land.getStatus());
            boolean timeReached = land.getHarvestTime() != null && !land.getHarvestTime().isAfter(now);
            if (!mature && !timeReached) {
                continue;
            }
            candidateLands.add(land);
        }
        if (candidateLands.isEmpty()) {
            return result;
        }

        List<Long> landIds = candidateLands.stream().map(FarmLand::getId).collect(Collectors.toList());
        Set<Long> stolenLandIds = findStolenLandIdsForCurrentCrop(stealerId, landIds);
        Map<Long, FarmPlantRecord> latestByLand = loadLatestUnharvestedPlantRecords(landIds);

        List<Long> cropIds = latestByLand.values().stream()
                .map(FarmPlantRecord::getCropId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, FarmCrop> cropMap = cropIds.isEmpty()
                ? Collections.emptyMap()
                : cropMapper.selectBatchIds(cropIds).stream()
                        .collect(Collectors.toMap(FarmCrop::getId, Function.identity()));

        for (FarmLand land : candidateLands) {
            if (stolenLandIds.contains(land.getId())) {
                continue;
            }
            FarmPlantRecord record = latestByLand.get(land.getId());
            if (record == null || FarmYesNoEnum.isYes(record.getHarvested())) {
                continue;
            }
            FarmCrop crop = cropMap.get(record.getCropId());
            if (crop == null) {
                continue;
            }
            if (remainingStealablePoints(crop, record) > 0) {
                result.put(land.getId(), true);
            }
        }
        return result;
    }

    private Map<Long, FarmPlantRecord> loadLatestUnharvestedPlantRecords(List<Long> landIds) {
        if (CollectionUtils.isEmpty(landIds)) {
            return Collections.emptyMap();
        }
        List<FarmPlantRecord> records = plantRecordMapper.selectList(new LambdaQueryWrapper<FarmPlantRecord>()
                .in(FarmPlantRecord::getLandId, landIds)
                .eq(FarmPlantRecord::getHarvested, FarmYesNoEnum.NO.getValue())
                .orderByDesc(FarmPlantRecord::getId));
        Map<Long, FarmPlantRecord> latestByLand = new HashMap<>();
        for (FarmPlantRecord record : records) {
            latestByLand.putIfAbsent(record.getLandId(), record);
        }
        return latestByLand;
    }

    private FarmPlantRecord getCurrentPlantRecord(Long landId) {
        return plantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                .eq(FarmPlantRecord::getLandId, landId)
                .eq(FarmPlantRecord::getHarvested, FarmYesNoEnum.NO.getValue())
                .orderByDesc(FarmPlantRecord::getId)
                .last("LIMIT 1"));
    }

    @Override
    public boolean validateFriend(Long stealerUserId, Long ownerUserId) {
        return userFollowService.isMutualFollow(stealerUserId, ownerUserId);
    }

    @Override
    public void updateTaskProgress(Long stealerId) {
        farmTaskService.updateTaskProgress(FarmTaskTypeEnum.STEAL);
    }

    @Override
    public List<FarmStealRecord> getStealRecordsByStealer(Long stealerId) {
        return stealRecordMapper.selectList(new LambdaQueryWrapper<FarmStealRecord>()
                .eq(FarmStealRecord::getStealerId, stealerId));
    }

    @Override
    public List<FarmStealRecordVO> getStealRecordsByOwner(Long ownerId) {
        return stealRecordMapper.selectStealRecordsWithStealerInfo(ownerId);
    }
}
