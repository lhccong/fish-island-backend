package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.mapper.farm.FarmStealRecordMapper;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.entity.farm.*;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.FarmFriendService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmUserService;
import com.cong.fishisland.service.UserFollowService;
import com.cong.fishisland.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmFriendServiceImpl implements FarmFriendService {

    private static final int STEAL_COOLDOWN_MINUTES = 10;

    @Resource
    private FarmLandService farmLandService;

    @Resource
    private FarmUserMapper farmUserMapper;

    @Resource
    private FarmCropMapper farmCropMapper;

    @Resource
    private FarmLandMapper farmLandMapper;

    @Resource
    private FarmPlantRecordMapper farmPlantRecordMapper;

    @Resource
    private FarmStealRecordMapper farmStealRecordMapper;

    @Resource
    private FarmUserService farmUserService;

    @Resource
    private UserService userService;

    @Resource
    private UserFollowService userFollowService;

    @Override
    public List<FarmFriendListVO> getFriendsWithStealStatus(Long farmUserId, Long systemUserId) {
        List<Long> mutualSystemUserIds = userFollowService.listMutualFollowUserIds(systemUserId);
        if (CollectionUtils.isEmpty(mutualSystemUserIds)) {
            return new ArrayList<>();
        }

        List<FarmUser> farmUsers = farmUserMapper.selectList(new LambdaQueryWrapper<FarmUser>()
                .in(FarmUser::getUserId, mutualSystemUserIds));
        if (CollectionUtils.isEmpty(farmUsers)) {
            return new ArrayList<>();
        }

        List<Long> friendFarmIds = farmUsers.stream().map(FarmUser::getId).collect(Collectors.toList());
        Map<Long, FarmUser> farmUserByFarmId = farmUsers.stream()
                .collect(Collectors.toMap(FarmUser::getId, Function.identity()));
        Map<Long, Long> systemUserIdByFarmId = farmUsers.stream()
                .collect(Collectors.toMap(FarmUser::getId, FarmUser::getUserId));

        Map<Long, User> systemUserMap = userService.listByIds(mutualSystemUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FarmLand> allLands = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .in(FarmLand::getUserId, friendFarmIds));
        List<Long> landIds = allLands.stream().map(FarmLand::getId).collect(Collectors.toList());
        List<FarmPlantRecord> allRecords = landIds.isEmpty()
                ? Collections.emptyList()
                : farmPlantRecordMapper.selectList(new LambdaQueryWrapper<FarmPlantRecord>()
                        .in(FarmPlantRecord::getLandId, landIds));
        Map<Long, FarmPlantRecord> recordMap = allRecords.stream()
                .collect(Collectors.toMap(FarmPlantRecord::getLandId, r -> r, (a, b) -> a));

        List<Long> cropIds = allLands.stream()
                .map(FarmLand::getPlantedCropId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, FarmCrop> cropMap = cropIds.isEmpty()
                ? Collections.emptyMap()
                : farmCropMapper.selectBatchIds(cropIds).stream()
                        .collect(Collectors.toMap(FarmCrop::getId, c -> c));

        Map<Long, LocalDateTime> stealCooldownMap = batchStealCooldownEnd(farmUserId, friendFarmIds);
        Map<Long, Boolean> canStealMap = batchCanSteal(farmUserId, friendFarmIds, stealCooldownMap, allLands, recordMap, cropMap);

        return friendFarmIds.stream().map(friendFarmId -> {
            FarmFriendListVO vo = new FarmFriendListVO();
            vo.setFriendId(friendFarmId);
            vo.setSystemUserId(systemUserIdByFarmId.get(friendFarmId));

            FarmUser farmUser = farmUserByFarmId.get(friendFarmId);
            if (farmUser != null) {
                vo.setLevel(farmUser.getLevel());
                User systemUser = systemUserMap.get(farmUser.getUserId());
                if (systemUser != null) {
                    vo.setNickname(systemUser.getUserName());
                    vo.setAvatar(systemUser.getUserAvatar());
                }
            }

            vo.setStealCooldown(stealCooldownMap.get(friendFarmId));
            vo.setCanSteal(canStealMap.getOrDefault(friendFarmId, false));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public int getFriendCount(Long systemUserId) {
        return (int) userFollowService.countMutualFollows(systemUserId);
    }

    @Override
    public boolean isMutualFriend(Long systemUserId, Long targetSystemUserId) {
        return userFollowService.isMutualFollow(systemUserId, targetSystemUserId);
    }

    @Override
    public boolean canSteal(Long farmUserId, Long friendFarmUserId, Long systemUserId, Long targetSystemUserId) {
        if (!isMutualFriend(systemUserId, targetSystemUserId)) {
            return false;
        }
        LocalDateTime cooldownEnd = getStealCooldownEnd(farmUserId, friendFarmUserId);
        if (cooldownEnd != null && cooldownEnd.isAfter(LocalDateTime.now())) {
            return false;
        }
        return hasStealableLand(friendFarmUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmFriendFarmVO visitFriendFarm(Long farmUserId, Long friendFarmUserId,
                                           Long systemUserId, Long targetSystemUserId) {
        if (!isMutualFriend(systemUserId, targetSystemUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可访问互相关注用户的农场");
        }

        FarmUser farmUser = farmUserMapper.selectOne(new LambdaQueryWrapper<FarmUser>()
                .eq(FarmUser::getId, friendFarmUserId)
                .last("LIMIT 1"));
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友农场用户不存在");
        }

        farmUserService.incrementVisitedCount(friendFarmUserId);

        List<FarmLand> lands = farmLandService.getLandsByUserId(friendFarmUserId);
        List<LandDTO> landDTOs = convertToLandDTOs(lands);

        boolean canSteal = canSteal(farmUserId, friendFarmUserId, systemUserId, targetSystemUserId);

        LocalDateTime cooldownEnd = getStealCooldownEnd(farmUserId, friendFarmUserId);
        Integer cooldownMinutes = null;
        LocalDateTime now = LocalDateTime.now();
        if (cooldownEnd != null && cooldownEnd.isAfter(now)) {
            cooldownMinutes = (int) Duration.between(now, cooldownEnd).toMinutes();
        }

        User systemUser = userService.getById(farmUser.getUserId());

        FarmFriendFarmVO vo = new FarmFriendFarmVO();
        vo.setFriendId(friendFarmUserId);
        if (systemUser != null) {
            vo.setFriendName(systemUser.getUserName());
            vo.setFriendAvatar(systemUser.getUserAvatar());
        }
        vo.setLands(landDTOs);
        vo.setCanSteal(canSteal);
        vo.setLastVisitTime(now);
        vo.setStealCooldownMinutes(cooldownMinutes);

        return vo;
    }

    private Map<Long, LocalDateTime> batchStealCooldownEnd(Long farmUserId, List<Long> friendFarmIds) {
        Map<Long, LocalDateTime> result = new HashMap<>();
        if (CollectionUtils.isEmpty(friendFarmIds)) {
            return result;
        }
        List<FarmStealRecord> records = farmStealRecordMapper.selectList(
                new LambdaQueryWrapper<FarmStealRecord>()
                        .eq(FarmStealRecord::getStealerId, farmUserId)
                        .in(FarmStealRecord::getOwnerId, friendFarmIds)
                        .orderByDesc(FarmStealRecord::getStolenTime));
        for (FarmStealRecord record : records) {
            result.putIfAbsent(record.getOwnerId(),
                    record.getStolenTime().plusMinutes(STEAL_COOLDOWN_MINUTES));
        }
        return result;
    }

    private LocalDateTime getStealCooldownEnd(Long farmUserId, Long friendFarmUserId) {
        FarmStealRecord last = farmStealRecordMapper.selectOne(new LambdaQueryWrapper<FarmStealRecord>()
                .eq(FarmStealRecord::getStealerId, farmUserId)
                .eq(FarmStealRecord::getOwnerId, friendFarmUserId)
                .orderByDesc(FarmStealRecord::getStolenTime)
                .last("LIMIT 1"));
        if (last == null || last.getStolenTime() == null) {
            return null;
        }
        return last.getStolenTime().plusMinutes(STEAL_COOLDOWN_MINUTES);
    }

    private Map<Long, Boolean> batchCanSteal(Long farmUserId, List<Long> friendFarmIds,
                                             Map<Long, LocalDateTime> stealCooldownMap,
                                             List<FarmLand> allLands,
                                             Map<Long, FarmPlantRecord> recordMap,
                                             Map<Long, FarmCrop> cropMap) {
        Map<Long, Boolean> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<FarmLand>> landsByFriend = allLands.stream()
                .collect(Collectors.groupingBy(FarmLand::getUserId));

        for (Long friendFarmId : friendFarmIds) {
            LocalDateTime cooldownEnd = stealCooldownMap.get(friendFarmId);
            if (cooldownEnd != null && cooldownEnd.isAfter(now)) {
                result.put(friendFarmId, false);
                continue;
            }
            result.put(friendFarmId, hasStealableLand(friendFarmId, landsByFriend.get(friendFarmId), recordMap, cropMap));
        }
        return result;
    }

    private boolean hasStealableLand(Long friendFarmUserId) {
        List<FarmLand> lands = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, friendFarmUserId)
                .orderByAsc(FarmLand::getLandIndex));
        return computeStealable(lands);
    }

    private boolean hasStealableLand(Long friendFarmUserId, List<FarmLand> lands,
                                   Map<Long, FarmPlantRecord> recordMap, Map<Long, FarmCrop> cropMap) {
        if (CollectionUtils.isEmpty(lands)) {
            return false;
        }
        if (!recordMap.isEmpty() || !cropMap.isEmpty()) {
            return computeStealableWithMaps(lands, recordMap, cropMap);
        }
        return computeStealable(lands);
    }

    private boolean computeStealable(List<FarmLand> lands) {
        if (CollectionUtils.isEmpty(lands)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        for (FarmLand land : lands) {
            if (land.getStatus() < 1 || land.getPlantedCropId() == null) {
                continue;
            }
            if (land.getHarvestTime() != null && land.getHarvestTime().isAfter(now)) {
                continue;
            }
            FarmCrop crop = farmCropMapper.selectById(land.getPlantedCropId());
            if (crop == null) {
                continue;
            }
            FarmPlantRecord record = farmPlantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                    .eq(FarmPlantRecord::getLandId, land.getId())
                    .eq(FarmPlantRecord::getHarvested, 0)
                    .last("LIMIT 1"));
            if (record == null) {
                continue;
            }
            if (remainingStealable(crop, record) > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean computeStealableWithMaps(List<FarmLand> lands,
                                             Map<Long, FarmPlantRecord> recordMap,
                                             Map<Long, FarmCrop> cropMap) {
        LocalDateTime now = LocalDateTime.now();
        for (FarmLand land : lands) {
            if (land.getStatus() < 1 || land.getPlantedCropId() == null) {
                continue;
            }
            if (land.getHarvestTime() != null && land.getHarvestTime().isAfter(now)) {
                continue;
            }
            FarmCrop crop = cropMap.get(land.getPlantedCropId());
            if (crop == null) {
                continue;
            }
            FarmPlantRecord record = recordMap.get(land.getId());
            if (record == null) {
                continue;
            }
            if (remainingStealable(crop, record) > 0) {
                return true;
            }
        }
        return false;
    }

    private int remainingStealable(FarmCrop crop, FarmPlantRecord record) {
        int baseReward = record.getPlantedPointsReward() != null ? record.getPlantedPointsReward() : crop.getCoin();
        int currentStolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
        int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
        return (baseReward - minReward) - currentStolenPoints;
    }

    private List<LandDTO> convertToLandDTOs(List<FarmLand> lands) {
        if (CollectionUtils.isEmpty(lands)) {
            return new ArrayList<>();
        }

        List<Long> cropIds = lands.stream()
                .map(FarmLand::getPlantedCropId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, FarmCrop> cropMap = cropIds.isEmpty()
                ? new HashMap<>()
                : farmCropMapper.selectBatchIds(cropIds).stream()
                        .collect(Collectors.toMap(FarmCrop::getId, Function.identity()));

        List<Long> landIds = lands.stream().map(FarmLand::getId).collect(Collectors.toList());
        List<FarmPlantRecord> records = farmPlantRecordMapper.selectList(new LambdaQueryWrapper<FarmPlantRecord>()
                .in(FarmPlantRecord::getLandId, landIds));
        Map<Long, FarmPlantRecord> recordMap = records.stream()
                .collect(Collectors.toMap(FarmPlantRecord::getLandId, r -> r, (a, b) -> a));

        return lands.stream().map(land -> {
            LandDTO dto = new LandDTO();
            dto.setId(land.getId());
            dto.setLandIndex(land.getLandIndex());
            dto.setStatus(land.getStatus());
            dto.setPlantedCropId(land.getPlantedCropId());
            dto.setPlantedTime(land.getPlantedTime());
            dto.setHarvestTime(land.getHarvestTime());
            dto.setLocked(land.getLocked());

            if (land.getPlantedCropId() != null) {
                FarmCrop crop = cropMap.get(land.getPlantedCropId());
                if (crop != null) {
                    dto.setCropName(crop.getName());
                }
                FarmPlantRecord record = recordMap.get(land.getId());
                if (record != null) {
                    dto.setPlantRecordId(record.getId());
                }
            }

            dto.setCanSteal(canStealLand(land, cropMap, recordMap));
            return dto;
        }).collect(Collectors.toList());
    }

    private boolean canStealLand(FarmLand land, Map<Long, FarmCrop> cropMap, Map<Long, FarmPlantRecord> recordMap) {
        if (land.getStatus() < 1 || land.getPlantedCropId() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (land.getHarvestTime() != null && land.getHarvestTime().isAfter(now)) {
            return false;
        }
        FarmCrop crop = cropMap.get(land.getPlantedCropId());
        if (crop == null) {
            crop = farmCropMapper.selectById(land.getPlantedCropId());
        }
        if (crop == null) {
            return false;
        }
        FarmPlantRecord record = recordMap.get(land.getId());
        if (record == null) {
            record = farmPlantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                    .eq(FarmPlantRecord::getLandId, land.getId())
                    .eq(FarmPlantRecord::getHarvested, 0)
                    .last("LIMIT 1"));
        }
        if (record == null) {
            return false;
        }
        return remainingStealable(crop, record) > 0;
    }
}
