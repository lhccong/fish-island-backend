package com.cong.fishisland.service.impl.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmFriendMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.mapper.farm.FarmStealRecordMapper;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmFriend;
import com.cong.fishisland.model.entity.farm.FarmPlantRecord;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.FARM_STEAL;

@Service
public class FarmStealServiceImpl implements FarmStealService {

    @Autowired
    private FarmStealRecordMapper stealRecordMapper;

    @Autowired
    private FarmPlantRecordMapper plantRecordMapper;

    @Autowired
    private FarmCropMapper cropMapper;

    @Autowired
    private UserPointsService userPointsService;

    @Autowired
    private UserPointsRecordService userPointsRecordService;

    @Autowired
    private FarmRankingService rankingService;

    @Autowired
    private FarmFriendMapper farmFriendMapper;

    @Autowired
    private FarmLandMapper farmLandMapper;

    @Autowired
    private FarmTaskService farmTaskService;

    @Autowired
    private FarmUserService farmUserService;

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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只能偷好友的作物");
        }

        if (!checkCooldown(stealerId, plantRecordId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "偷菜冷却中，请稍后再试");
        }

        int baseReward = plantRecord.getPlantedPointsReward() != null ? plantRecord.getPlantedPointsReward() : crop.getCoin();
        int currentStolenPoints = plantRecord.getStolenPoints() != null ? plantRecord.getStolenPoints() : 0;
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

        FarmStealRecord stealRecord = new FarmStealRecord();
        stealRecord.setStealerId(stealerId);
        stealRecord.setOwnerId(ownerId);
        stealRecord.setPlantRecordId(plantRecordId);
        stealRecord.setCropId(crop.getId());
        stealRecord.setStolenTime(LocalDateTime.now());
        stealRecord.setExpGained(stealPoints);
        stealRecord.setCoinGained(stealPoints);
        stealRecordMapper.insert(stealRecord);

        plantRecord.setStolenCount(plantRecord.getStolenCount() + 1);
        plantRecord.setStolenPoints(currentStolenPoints + stealPoints);
        plantRecordMapper.updateById(plantRecord);

        Long stealerSystemUserId = StpUtil.getLoginIdAsLong();
        UserPoints stealerPoints = userPointsService.getById(stealerSystemUserId);
        if (stealerPoints != null) {
            int beforePoints = stealerPoints.getPoints();
            int afterPoints = beforePoints + stealPoints;
            int usedPoints = stealerPoints.getUsedPoints() == null ? 0 : stealerPoints.getUsedPoints();

            userPointsService.updatePoints(stealerSystemUserId, stealPoints, false);

            userPointsRecordService.addPointsIncreaseRecord(stealerSystemUserId, stealPoints, FARM_STEAL.getValue(),
                    "偷取作物: " + crop.getName(),
                    beforePoints, afterPoints, usedPoints, usedPoints);
        }

        rankingService.updateStealRanking(stealerId, stealPoints);

        farmFriendMapper.updateStealCooldown(stealerId, ownerId, now.plusMinutes(10));

        farmUserService.incrementTotalSteal(stealerId);
        farmUserService.incrementTotalDefense(ownerId);

        farmTaskService.updateTaskProgress(stealerId, "steal");

        return stealRecord;
    }

    @Override
    public boolean checkCooldown(Long stealerId, Long plantRecordId) {
        FarmPlantRecord plantRecord = plantRecordMapper.selectById(plantRecordId);
        if (plantRecord == null) {
            return false;
        }

        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, stealerId)
                .eq(FarmFriend::getFriendId, plantRecord.getUserId())
                .eq(FarmFriend::getStatus, 1)
                .last("LIMIT 1"));
        if (friend == null) {
            return false;
        }

        if (friend.getStealCooldown() != null && friend.getStealCooldown().isAfter(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean validateFriend(Long stealerId, Long ownerId) {
        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, stealerId)
                .eq(FarmFriend::getFriendId, ownerId)
                .eq(FarmFriend::getStatus, 1)
                .last("LIMIT 1"));
        return friend != null;
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
