package com.cong.fishisland.service.impl.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.dto.farm.FarmUserVO;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.farm.FarmUserStatusEnum;
import com.cong.fishisland.service.FarmUserService;
import com.cong.fishisland.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmUserServiceImpl extends ServiceImpl<FarmUserMapper, FarmUser> implements FarmUserService {

    @Resource
    private UserService userService;

    @Override
    public FarmUser getFarmUserByUserId(Long systemUserId) {
        return getById(systemUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmUser createFarmUser(Long systemUserId) {
        FarmUser existing = getFarmUserByUserId(systemUserId);
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        FarmUser farmUser = FarmUser.builder()
                .userId(systemUserId)
                .level(1)
                .experience(0)
                .totalHarvest(0)
                .totalSteal(0)
                .totalDefense(0)
                .friendCount(0)
                .visitedCount(0)
                .status(FarmUserStatusEnum.NORMAL.getValue())
                .createTime(now)
                .updateTime(now)
                .build();
        save(farmUser);
        return farmUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmUser getOrCreateFarmUser() {
        return getOrCreateFarmUser(StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmUser getOrCreateFarmUser(Long systemUserId) {
        FarmUser farmUser = getFarmUserByUserId(systemUserId);
        if (farmUser == null) {
            return createFarmUser(systemUserId);
        }
        return farmUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmUserVO getFarmUserVO(Long systemUserId) {
        FarmUser farmUser = getOrCreateFarmUser(systemUserId);
        return toVO(farmUser);
    }

    @Override
    public FarmUserVO toVO(FarmUser farmUser) {
        if (farmUser == null) {
            return null;
        }
        User user = userService.getById(farmUser.getUserId());
        return FarmUserVO.from(farmUser, user);
    }

    @Override
    public List<FarmUserVO> toVOList(List<FarmUser> farmUsers) {
        if (farmUsers == null || farmUsers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> systemUserIds = farmUsers.stream()
                .map(FarmUser::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userService.listByIds(systemUserIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return farmUsers.stream()
                .map(fu -> FarmUserVO.from(fu, userMap.get(fu.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public boolean addExperience(Long userId, Integer exp) {
        if (exp <= 0) {
            return false;
        }
        int result = baseMapper.addExperience(userId, exp);
        if (result > 0) {
            FarmUser farmUser = getById(userId);
            if (farmUser != null) {
                int newLevel = calculateLevel(farmUser.getExperience());
                if (newLevel > farmUser.getLevel()) {
                    baseMapper.updateLevel(userId, newLevel);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int calculateLevel(Integer experience) {
        if (experience == null || experience < 0) {
            return 1;
        }
        return (experience / 100) + 1;
    }

    @Override
    public void incrementTotalHarvest(Long userId) {
        baseMapper.incrementTotalHarvest(userId);
    }

    @Override
    public void incrementTotalSteal(Long userId) {
        baseMapper.incrementTotalSteal(userId);
    }

    @Override
    public void incrementTotalDefense(Long userId) {
        baseMapper.incrementTotalDefense(userId);
    }

    @Override
    public boolean incrementVisitedCount(Long userId) {
        return baseMapper.incrementVisitedCount(userId) > 0;
    }

    @Override
    public List<FarmUser> getFarmUsersByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        return listByIds(userIds);
    }
}
