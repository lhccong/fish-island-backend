package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.mapper.farm.FarmStealRecordMapper;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FarmStealServiceImpl implements FarmStealService {

    private static final int STEAL_COOLDOWN_MINUTES = 10;

    @Autowired
    private FarmStealRecordMapper stealRecordMapper;

    @Autowired
    private FarmPlantRecordMapper plantRecordMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmStealRecord steal(Long stealerId, Long plantRecordId) {
        FarmPlantRecord plantRecord = plantRecordMapper.selectById(plantRecordId);
        if (plantRecord == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "种植记录不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (plantRecord.getHarvested() == 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物已被收获");
        }
        if (plantRecord.getHarvestTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物尚未成熟");
        }

        if (plantRecord.getStolenCount() >= 3) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该作物已被偷3次，无法再偷");
        }

        FarmCrop crop = cropMapper.selectById(plantRecord.getCropId());
        if (crop == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作物信息不存在");
        }

        Long ownerId = plantRecord.getUserId();
        if (stealerId.equals(ownerId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能偷自己的作物");
        }

        if (!validateFriend(stealerId, ownerId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能偷互相关注用户的作物");
        }

        if (!checkCooldown(stealerId, plantRecordId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "偷菜冷却中，请稍后再试");
        }

        int baseReward = plantRecord.getPlantedPointsReward() != null ? plantRecord.getPlantedPointsReward() : crop.getCoin();
        int currentStolenPoints = plantRecord.getStolenPoints() != null ? plantRecord.getStolenPoints() : 0;
        int stealPoints = getStealPoints(crop, baseReward, currentStolenPoints);

        FarmStealRecord stealRecord = new FarmStealRecord();
        stealRecord.setStealerId(stealerId);
        stealRecord.setOwnerId(ownerId);
        stealRecord.setPlantRecordId(plantRecordId);
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

        farmTaskService.updateTaskProgress(stealerId, "steal");

        return stealRecord;
    }

    private static int getStealPoints(FarmCrop crop, int baseReward, int currentStolenPoints) {
        int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
        int maxStealableTotal = baseReward - minReward;
        int remainingStealable = maxStealableTotal - currentStolenPoints;

        if (remainingStealable <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该作物已无可偷积分");
        }

        int stealPoints = Math.min(crop.getCoin() / 2, remainingStealable);
        if (stealPoints <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "可偷积分不足");
        }
        return stealPoints;
    }

    @Override
    public boolean checkCooldown(Long stealerId, Long plantRecordId) {
        FarmPlantRecord plantRecord = plantRecordMapper.selectById(plantRecordId);
        if (plantRecord == null) {
            return false;
        }

        if (!validateFriend(stealerId, plantRecord.getUserId())) {
            return false;
        }

        FarmStealRecord last = stealRecordMapper.selectOne(new LambdaQueryWrapper<FarmStealRecord>()
                .eq(FarmStealRecord::getStealerId, stealerId)
                .eq(FarmStealRecord::getOwnerId, plantRecord.getUserId())
                .orderByDesc(FarmStealRecord::getStolenTime)
                .last("LIMIT 1"));
        if (last == null || last.getStolenTime() == null) {
            return true;
        }
        return !last.getStolenTime().plusMinutes(STEAL_COOLDOWN_MINUTES).isAfter(LocalDateTime.now());
    }

    @Override
    public boolean validateFriend(Long stealerFarmUserId, Long ownerFarmUserId) {
        FarmUser stealer = farmUserService.getById(stealerFarmUserId);
        FarmUser owner = farmUserService.getById(ownerFarmUserId);
        if (stealer == null || owner == null) {
            return false;
        }
        return userFollowService.isMutualFollow(stealer.getUserId(), owner.getUserId());
    }

    @Override
    public void updateTaskProgress(Long stealerId) {
        farmTaskService.updateTaskProgress(stealerId, "steal");
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
