package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmFriendMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmPlantRecordMapper;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.entity.farm.*;
import com.cong.fishisland.service.FarmCropService;
import com.cong.fishisland.service.FarmFriendService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmFriendServiceImpl extends ServiceImpl<FarmFriendMapper, FarmFriend> implements FarmFriendService {

    @Resource
    private FarmFriendMapper farmFriendMapper;

    @Resource
    private FarmLandService farmLandService;

    @Resource
    private FarmCropService farmCropService;

    @Resource
    private FarmUserMapper farmUserMapper;

    @Resource
    private FarmCropMapper farmCropMapper;

    @Resource
    private FarmLandMapper farmLandMapper;

    @Resource
    private FarmPlantRecordMapper farmPlantRecordMapper;

    @Resource
    private FarmUserService farmUserService;

    @Override
    public List<FarmFriend> getFriendsByUserId(Long userId) {
        return farmFriendMapper.selectList(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getStatus, 1)
                .orderByDesc(FarmFriend::getCreatedAt));
    }

    @Override
    public List<FarmFriendListVO> getFriendsWithStealStatus(Long userId) {
        List<FarmFriend> friends = farmFriendMapper.selectList(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getStatus, 1)
                .orderByDesc(FarmFriend::getCreatedAt));
        if (CollectionUtils.isEmpty(friends)) {
            return List.of();
        }

        List<Long> friendIds = friends.stream()
                .map(FarmFriend::getFriendId)
                .collect(Collectors.toList());

        List<FarmUser> farmUsers = farmUserMapper.selectList(new LambdaQueryWrapper<FarmUser>()
                .in(FarmUser::getId, friendIds));
        Map<Long, FarmUser> userMap = farmUsers.stream()
                .collect(Collectors.toMap(FarmUser::getId, Function.identity()));

        List<FarmLand> allLands = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .in(FarmLand::getUserId, friendIds));
        List<FarmPlantRecord> allRecords = farmPlantRecordMapper.selectList(new LambdaQueryWrapper<FarmPlantRecord>()
                .in(FarmPlantRecord::getLandId, allLands.stream().map(FarmLand::getId).collect(Collectors.toList())));
        Map<Long, FarmPlantRecord> recordMap = allRecords.stream()
                .collect(Collectors.toMap(FarmPlantRecord::getLandId, r -> r));
        List<FarmCrop> allCrops = farmCropMapper.selectBatchIds(
                allLands.stream().map(FarmLand::getPlantedCropId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList()));
        Map<Long, FarmCrop> cropMap = allCrops.stream()
                .collect(Collectors.toMap(FarmCrop::getId, c -> c));

        Map<Long, Boolean> canStealMap = batchCanSteal(userId, friends, allLands, recordMap, cropMap);

        LocalDateTime now = LocalDateTime.now();
        return friends.stream().map(friend -> {
            FarmFriendListVO vo = new FarmFriendListVO();
            vo.setId(friend.getId());
            vo.setFriendId(friend.getFriendId());
            vo.setStatus(friend.getStatus());
            vo.setLastVisitTime(friend.getLastVisitTime());
            vo.setStealCooldown(friend.getStealCooldown());

            FarmUser farmUser = userMap.get(friend.getFriendId());
            if (farmUser != null) {
                vo.setNickname(farmUser.getNickname());
                vo.setAvatar(farmUser.getAvatar());
                vo.setLevel(farmUser.getLevel());
            }

            vo.setCanSteal(canStealMap.getOrDefault(friend.getFriendId(), false));

            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, Boolean> batchCanSteal(Long userId, List<FarmFriend> friends,
                                             List<FarmLand> allLands,
                                             Map<Long, FarmPlantRecord> recordMap,
                                             Map<Long, FarmCrop> cropMap) {
        Map<Long, Boolean> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, FarmFriend> friendMap = friends.stream()
                .collect(Collectors.toMap(FarmFriend::getFriendId, f -> f));

        Map<Long, List<FarmLand>> landsByFriend = allLands.stream()
                .collect(Collectors.groupingBy(FarmLand::getUserId));

        for (FarmFriend friend : friends) {
            Long friendId = friend.getFriendId();

            if (friend.getStatus() != 1) {
                result.put(friendId, false);
                continue;
            }

            if (friend.getStealCooldown() != null && friend.getStealCooldown().isAfter(now)) {
                result.put(friendId, false);
                continue;
            }

            List<FarmLand> lands = landsByFriend.get(friendId);
            if (CollectionUtils.isEmpty(lands)) {
                result.put(friendId, false);
                continue;
            }

            boolean hasStealable = false;
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

                int baseReward = record.getPlantedPointsReward() != null ? record.getPlantedPointsReward() : crop.getCoin();
                int currentStolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
                int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
                int remainingStealable = (baseReward - minReward) - currentStolenPoints;

                if (remainingStealable > 0) {
                    hasStealable = true;
                    break;
                }
            }
            result.put(friendId, hasStealable);
        }
        return result;
    }

    @Override
    public FarmFriend getFriend(Long userId, Long friendId) {
        return farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .last("LIMIT 1"));
    }

    @Override
    public int getFriendCount(Long userId) {
        return farmFriendMapper.selectCount(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getStatus, 1)).intValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmFriend addFriend(Long userId, Long friendId) {
        FarmFriend existing = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .last("LIMIT 1"));
        if (existing != null) {
            if (existing.getStatus() == 0) {
                existing.setStatus(1);
                existing.setUpdatedAt(LocalDateTime.now());
                farmFriendMapper.updateById(existing);
                return existing;
            }
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        FarmFriend farmFriend = FarmFriend.builder()
                .userId(userId)
                .friendId(friendId)
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        farmFriendMapper.insert(farmFriend);

        farmUserService.incrementFriendCount(userId);
        farmUserService.incrementFriendCount(friendId);

        return farmFriend;
    }

    @Override
    public boolean removeFriend(Long userId, Long friendId) {
        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .last("LIMIT 1"));
        if (friend == null) {
            return false;
        }
        boolean deleted = farmFriendMapper.deleteById(friend.getId()) > 0;
        if (deleted) {
            farmUserMapper.updateFriendCount(userId, -1);
            farmUserMapper.updateFriendCount(friendId, -1);
        }
        return deleted;
    }

    @Override
    public boolean blockFriend(Long userId, Long friendId) {
        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .last("LIMIT 1"));
        if (friend == null) {
            return false;
        }
        friend.setStatus(0);
        friend.setUpdatedAt(LocalDateTime.now());
        return farmFriendMapper.updateById(friend) > 0;
    }

    @Override
    public boolean unblockFriend(Long userId, Long friendId) {
        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .last("LIMIT 1"));
        if (friend == null || friend.getStatus() == 1) {
            return false;
        }
        friend.setStatus(1);
        friend.setUpdatedAt(LocalDateTime.now());
        return farmFriendMapper.updateById(friend) > 0;
    }

    /**
     * 更新最后访问时间
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     * @return 是否更新成功
     */
    @Override
    public boolean updateLastVisitTime(Long userId, Long friendId) {
        return farmFriendMapper.updateLastVisitTime(userId, friendId, LocalDateTime.now()) > 0;
    }

    /**
     * 更新偷菜冷却时间
     *
     * @param userId       用户ID
     * @param friendId     好友ID
     * @param cooldownTime 冷却时间
     * @return 是否更新成功
     */
    @Override
    public boolean updateStealCooldown(Long userId, Long friendId, LocalDateTime cooldownTime) {
        return farmFriendMapper.updateStealCooldown(userId, friendId, cooldownTime) > 0;
    }

    @Override
    public boolean canSteal(Long userId, Long friendId) {
        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .eq(FarmFriend::getStatus, 1)
                .last("LIMIT 1"));
        if (friend == null) {
            return false;
        }

        if (friend.getStealCooldown() != null && friend.getStealCooldown().isAfter(LocalDateTime.now())) {
            return false;
        }

        List<FarmLand> lands = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, friendId)
                .orderByAsc(FarmLand::getLandIndex));
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

            int baseReward = record.getPlantedPointsReward() != null ? record.getPlantedPointsReward() : crop.getCoin();
            int currentStolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
            int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
            int maxStealableTotal = baseReward - minReward;
            int remainingStealable = maxStealableTotal - currentStolenPoints;

            if (remainingStealable > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<FarmFriend> getActiveFriends(Long userId) {
        return farmFriendMapper.selectList(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getStatus, 1)
                .orderByDesc(FarmFriend::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmFriendFarmVO visitFriendFarm(Long userId, Long friendId) {
        FarmFriend friend = farmFriendMapper.selectOne(new LambdaQueryWrapper<FarmFriend>()
                .eq(FarmFriend::getUserId, userId)
                .eq(FarmFriend::getFriendId, friendId)
                .last("LIMIT 1"));
        if (friend == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友关系不存在");
        }
        if (friend.getStatus() == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该好友已被拉黑");
        }

        FarmUser farmUser = farmUserMapper.selectOne(new LambdaQueryWrapper<FarmUser>()
                .eq(FarmUser::getId, friendId)
                .last("LIMIT 1"));
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友农场用户不存在");
        }

        farmFriendMapper.updateLastVisitTime(userId, friendId, LocalDateTime.now());
        farmUserService.incrementVisitedCount(friendId);

        List<FarmLand> lands = farmLandService.getLandsByUserId(friendId);
        List<LandDTO> landDTOs = convertToLandDTOs(lands);

        boolean canSteal = canSteal(userId, friendId);

        Integer cooldownMinutes = null;
        if (friend.getStealCooldown() != null && friend.getStealCooldown().isAfter(LocalDateTime.now())) {
            long minutes = Duration.between(LocalDateTime.now(), friend.getStealCooldown()).toMinutes();
            cooldownMinutes = (int) minutes;
        }

        FarmFriendFarmVO vo = new FarmFriendFarmVO();
        vo.setFriendId(friendId);
        vo.setFriendName(farmUser.getNickname());
        vo.setFriendAvatar(farmUser.getAvatar());
        vo.setLands(landDTOs);
        vo.setCanSteal(canSteal);
        vo.setLastVisitTime(LocalDateTime.now());
        vo.setStealCooldownMinutes(cooldownMinutes);

        return vo;
    }

    private List<LandDTO> convertToLandDTOs(List<FarmLand> lands) {
        if (CollectionUtils.isEmpty(lands)) {
            return List.of();
        }

        List<Long> cropIds = lands.stream()
                .map(FarmLand::getPlantedCropId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, FarmCrop> cropMap;
        if (!cropIds.isEmpty()) {
            List<FarmCrop> crops = farmCropMapper.selectBatchIds(cropIds);
            cropMap = crops.stream().collect(Collectors.toMap(FarmCrop::getId, Function.identity()));
        } else {
            cropMap = new HashMap<>();
        }

        List<Long> landIds = lands.stream().map(FarmLand::getId).collect(Collectors.toList());
        List<FarmPlantRecord> records = farmPlantRecordMapper.selectList(new LambdaQueryWrapper<FarmPlantRecord>()
                .in(FarmPlantRecord::getLandId, landIds));
        Map<Long, FarmPlantRecord> recordMap = records.stream()
                .collect(Collectors.toMap(FarmPlantRecord::getLandId, r -> r));

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

            dto.setCanSteal(canStealLand(land));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 判断单个地块是否可偷菜
     * 条件：1.作物已成熟 2.已过收获时间 3.种植记录存在且未收获 4.剩余可偷积分>0
     *
     * @param land 地块实体
     * @return 是否可偷菜
     */
    private boolean canStealLand(FarmLand land) {
        if (land.getStatus() < 1 || land.getPlantedCropId() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (land.getHarvestTime() != null && land.getHarvestTime().isAfter(now)) {
            return false;
        }

        FarmCrop crop = farmCropMapper.selectById(land.getPlantedCropId());
        if (crop == null) {
            return false;
        }

        FarmPlantRecord record = farmPlantRecordMapper.selectOne(new LambdaQueryWrapper<FarmPlantRecord>()
                .eq(FarmPlantRecord::getLandId, land.getId())
                .eq(FarmPlantRecord::getHarvested, 0)
                .last("LIMIT 1"));
        if (record == null) {
            return false;
        }

        int baseReward = record.getPlantedPointsReward() != null ? record.getPlantedPointsReward() : crop.getCoin();
        int currentStolenPoints = record.getStolenPoints() != null ? record.getStolenPoints() : 0;
        int minReward = crop.getPrice() != null ? crop.getPrice() : 0;
        int maxStealableTotal = baseReward - minReward;
        int remainingStealable = maxStealableTotal - currentStolenPoints;

        return remainingStealable > 0;
    }
}
