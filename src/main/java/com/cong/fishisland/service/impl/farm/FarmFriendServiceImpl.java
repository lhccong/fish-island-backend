package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.mapper.farm.FarmLandMapper;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.farm.FarmConstants;
import com.cong.fishisland.model.enums.farm.FarmLandStatusEnum;
import com.cong.fishisland.service.FarmFriendService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmStealService;
import com.cong.fishisland.service.FarmUserService;
import com.cong.fishisland.service.UserFollowService;
import com.cong.fishisland.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmFriendServiceImpl implements FarmFriendService {

    @Resource
    private FarmLandService farmLandService;

    @Resource
    private FarmUserMapper farmUserMapper;

    @Resource
    private FarmCropMapper farmCropMapper;

    @Resource
    private FarmLandMapper farmLandMapper;

    @Resource
    private FarmUserService farmUserService;

    @Resource
    private UserService userService;

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private FarmStealService farmStealService;

    @Override
    public List<FarmFriendListVO> getFriendsWithStealStatus(Long systemUserId) {
        List<Long> mutualSystemUserIds = userFollowService.listMutualFollowUserIds(systemUserId);
        if (CollectionUtils.isEmpty(mutualSystemUserIds)) {
            return new ArrayList<>();
        }

        List<FarmUser> farmUsers = farmUserMapper.selectList(new LambdaQueryWrapper<FarmUser>()
                .in(FarmUser::getUserId, mutualSystemUserIds));
        if (CollectionUtils.isEmpty(farmUsers)) {
            return new ArrayList<>();
        }

        List<Long> friendUserIds = farmUsers.stream().map(FarmUser::getUserId).collect(Collectors.toList());
        Map<Long, FarmUser> farmUserByUserId = farmUsers.stream()
                .collect(Collectors.toMap(FarmUser::getUserId, Function.identity()));

        Map<Long, User> systemUserMap = userService.listByIds(mutualSystemUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FarmLand> allLands = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .in(FarmLand::getUserId, friendUserIds));
        Map<Long, Boolean> canStealMap = batchCanSteal(systemUserId, friendUserIds, allLands);

        return friendUserIds.stream().map(friendUserId -> {
            FarmFriendListVO vo = new FarmFriendListVO();
            vo.setFriendId(friendUserId);
            vo.setSystemUserId(friendUserId);

            FarmUser farmUser = farmUserByUserId.get(friendUserId);
            if (farmUser != null) {
                vo.setLevel(farmUser.getLevel());
                User systemUser = systemUserMap.get(friendUserId);
                if (systemUser != null) {
                    vo.setNickname(systemUser.getUserName());
                    vo.setAvatar(systemUser.getUserAvatar());
                }
            }

            vo.setCanSteal(canStealMap.getOrDefault(friendUserId, false));
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
    public boolean canSteal(Long systemUserId, Long targetSystemUserId) {
        if (!isMutualFriend(systemUserId, targetSystemUserId)) {
            return false;
        }
        List<FarmLand> lands = farmLandMapper.selectList(new LambdaQueryWrapper<FarmLand>()
                .eq(FarmLand::getUserId, targetSystemUserId));
        if (CollectionUtils.isEmpty(lands)) {
            return false;
        }
        return lands.stream().anyMatch(land -> farmStealService.canStealLand(systemUserId, land.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmFriendFarmVO visitFriendFarm(Long systemUserId, Long targetSystemUserId) {
        if (!isMutualFriend(systemUserId, targetSystemUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可访问互相关注用户的农场");
        }

        FarmUser farmUser = farmUserMapper.selectById(targetSystemUserId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友农场用户不存在");
        }

        farmUserService.incrementVisitedCount(targetSystemUserId);

        List<FarmLand> lands = farmLandService.getLandsByUserId(targetSystemUserId);
        List<LandDTO> landDTOs = convertToLandDTOs(lands, systemUserId);

        boolean canSteal = landDTOs.stream().anyMatch(dto -> Boolean.TRUE.equals(dto.getCanSteal()));

        User systemUser = userService.getById(targetSystemUserId);

        FarmFriendFarmVO vo = new FarmFriendFarmVO();
        vo.setFriendId(targetSystemUserId);
        if (systemUser != null) {
            vo.setFriendName(systemUser.getUserName());
            vo.setFriendAvatar(systemUser.getUserAvatar());
        }
        vo.setLands(landDTOs);
        vo.setCanSteal(canSteal);
        vo.setLastVisitTime(LocalDateTime.now());

        return vo;
    }

    @Override
    public List<LandDTO> getFriendLands(Long systemUserId, Long targetSystemUserId) {
        if (!isMutualFriend(systemUserId, targetSystemUserId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可访问互相关注用户的农场");
        }
        FarmUser farmUser = farmUserMapper.selectById(targetSystemUserId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "好友农场用户不存在");
        }
        List<FarmLand> lands = farmLandService.getLandsByUserId(targetSystemUserId);
        return convertToLandDTOs(lands, systemUserId);
    }

    private Map<Long, Boolean> batchCanSteal(Long stealerUserId, List<Long> friendUserIds, List<FarmLand> allLands) {
        Map<Long, Boolean> result = new HashMap<>();
        if (CollectionUtils.isEmpty(friendUserIds)) {
            return result;
        }
        Map<Long, List<FarmLand>> landsByFriend = allLands.stream()
                .collect(Collectors.groupingBy(FarmLand::getUserId));
        Map<Long, Boolean> canStealByLand = farmStealService.batchCanStealLand(stealerUserId, allLands);

        for (Long friendUserId : friendUserIds) {
            List<FarmLand> lands = landsByFriend.get(friendUserId);
            if (CollectionUtils.isEmpty(lands)) {
                result.put(friendUserId, false);
                continue;
            }
            boolean stealable = lands.stream()
                    .anyMatch(land -> Boolean.TRUE.equals(canStealByLand.get(land.getId())));
            result.put(friendUserId, stealable);
        }
        return result;
    }

    private List<LandDTO> convertToLandDTOs(List<FarmLand> lands, Long stealerUserId) {
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

        Map<Long, Boolean> canStealByLand = farmStealService.batchCanStealLand(stealerUserId, lands);
        LocalDateTime now = LocalDateTime.now();

        return lands.stream().map(land -> {
            LandDTO dto = new LandDTO();
            dto.setId(land.getId());
            dto.setLandIndex(land.getLandIndex());
            dto.setStatus(FarmLandStatusEnum.resolveDisplayStatus(
                    land.getStatus(), land.getHarvestTime(), now));
            dto.setPlantedCropId(land.getPlantedCropId());
            dto.setPlantedTime(land.getPlantedTime());
            dto.setHarvestTime(land.getHarvestTime());
            dto.setLocked(land.getLocked());
            if (land.getLandIndex() != null && FarmConstants.isLevelUnlockableLandIndex(land.getLandIndex())) {
                dto.setUnlockLevel(FarmConstants.unlockLevelForLandIndex(land.getLandIndex()));
                dto.setUnlockCost(FarmConstants.unlockCostForLandIndex(land.getLandIndex()));
            }

            if (land.getPlantedCropId() != null) {
                FarmCrop crop = cropMap.get(land.getPlantedCropId());
                if (crop != null) {
                    dto.setCropName(crop.getName());
                }
            }

            dto.setCanSteal(canStealByLand.getOrDefault(land.getId(), false));

            return dto;
        }).collect(Collectors.toList());
    }
}
