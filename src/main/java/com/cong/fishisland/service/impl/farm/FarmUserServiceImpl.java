package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.farm.FarmUserMapper;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.service.FarmUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FarmUserServiceImpl extends ServiceImpl<FarmUserMapper, FarmUser> implements FarmUserService {

    @Override
    public FarmUser getFarmUserByUserId(Long systemUserId) {
        return getOne(new LambdaQueryWrapper<FarmUser>()
                .eq(FarmUser::getUserId, systemUserId)
                .last("LIMIT 1"));
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
                .nickname("农场用户" + System.currentTimeMillis())
                .avatar("")
                .level(1)
                .experience(0)
                .totalHarvest(0)
                .totalSteal(0)
                .totalDefense(0)
                .friendCount(0)
                .visitedCount(0)
                .consecutiveDays(0)
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        save(farmUser);
        return farmUser;
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
    public Long getFarmUserId(Long systemUserId) {
        FarmUser farmUser = getOrCreateFarmUser(systemUserId);
        return farmUser.getId();
    }

    @Override
    public Long getSystemUserId(Long farmUserId) {
        FarmUser farmUser = getById(farmUserId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "农场用户不存在");
        }
        return farmUser.getUserId();
    }

    @Override
    public boolean addExperience(Long farmUserId, Integer exp) {
        if (exp <= 0) {
            return false;
        }
        int result = baseMapper.addExperience(farmUserId, exp);
        if (result > 0) {
            FarmUser farmUser = getById(farmUserId);
            if (farmUser != null) {
                int newLevel = calculateLevel(farmUser.getExperience());
                if (newLevel > farmUser.getLevel()) {
                    baseMapper.updateLevel(farmUserId, newLevel);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean addCoin(Long farmUserId, Integer coin) {
        if (coin <= 0) {
            return false;
        }
        return baseMapper.addCoin(farmUserId, coin) > 0;
    }

    @Override
    public boolean spendCoin(Long farmUserId, Integer coin) {
        if (coin <= 0) {
            return false;
        }
        FarmUser farmUser = getById(farmUserId);
        return baseMapper.addCoin(farmUserId, -coin) > 0;
    }

    @Override
    public boolean updateLevel(Long farmUserId, Integer level) {
        return baseMapper.updateLevel(farmUserId, level) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean signIn(Long farmUserId) {
        FarmUser farmUser = getById(farmUserId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "农场用户不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        if (farmUser.getLastSignInDate() != null) {
            LocalDate lastSignIn = farmUser.getLastSignInDate().toLocalDate();
            if (lastSignIn.equals(today)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "今日已签到");
            }

            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastSignIn, today);
            if (daysBetween == 1) {
                baseMapper.updateSignIn(farmUserId, now);
            } else {
                baseMapper.resetSignIn(farmUserId, now);
            }
        } else {
            baseMapper.updateSignIn(farmUserId, now);
        }

        addExperience(farmUserId, 5);
        return true;
    }

    @Override
    public boolean isSignedToday(Long farmUserId) {
        FarmUser farmUser = getById(farmUserId);
        if (farmUser == null || farmUser.getLastSignInDate() == null) {
            return false;
        }
        return farmUser.getLastSignInDate().toLocalDate().equals(LocalDate.now());
    }

    @Override
    public int calculateLevel(Integer experience) {
        if (experience == null || experience < 0) {
            return 1;
        }
        return (experience / 100) + 1;
    }

    @Override
    public boolean updateProfile(Long farmUserId, String nickname, String avatar) {
        FarmUser farmUser = getById(farmUserId);
        if (farmUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "农场用户不存在");
        }

        if (nickname != null && !nickname.trim().isEmpty()) {
            farmUser.setNickname(nickname.trim());
        }
        if (avatar != null) {
            farmUser.setAvatar(avatar);
        }
        farmUser.setUpdatedAt(LocalDateTime.now());
        return updateById(farmUser);
    }

    @Override
    public boolean incrementTotalHarvest(Long farmUserId) {
        return baseMapper.incrementTotalHarvest(farmUserId) > 0;
    }

    @Override
    public boolean incrementTotalSteal(Long farmUserId) {
        return baseMapper.incrementTotalSteal(farmUserId) > 0;
    }

    @Override
    public boolean incrementTotalDefense(Long farmUserId) {
        return baseMapper.incrementTotalDefense(farmUserId) > 0;
    }

    @Override
    public boolean incrementVisitedCount(Long farmUserId) {
        return baseMapper.incrementVisitedCount(farmUserId) > 0;
    }

    @Override
    public boolean incrementFriendCount(Long farmUserId) {
        return baseMapper.updateFriendCount(farmUserId, 1) > 0;
    }

    @Override
    public List<FarmUser> getFarmUsersByIds(List<Long> farmUserIds) {
        if (farmUserIds == null || farmUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        return listByIds(farmUserIds);
    }
}
