package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.farm.FarmUser;

import java.util.List;

public interface FarmUserService extends IService<FarmUser> {

    FarmUser getFarmUserByUserId(Long userId);

    FarmUser createFarmUser(Long userId);

    FarmUser getOrCreateFarmUser(Long userId);

    Long getFarmUserId(Long userId);

    Long getSystemUserId(Long farmUserId);

    boolean addExperience(Long userId, Integer exp);

    boolean addCoin(Long userId, Integer coin);

    boolean spendCoin(Long userId, Integer coin);

    boolean updateLevel(Long userId, Integer level);

    boolean signIn(Long userId);

    boolean isSignedToday(Long userId);

    int calculateLevel(Integer experience);

    boolean updateProfile(Long userId, String nickname, String avatar);

    boolean incrementTotalHarvest(Long userId);

    boolean incrementTotalSteal(Long userId);

    boolean incrementTotalDefense(Long userId);

    boolean incrementVisitedCount(Long userId);

    boolean incrementFriendCount(Long userId);

    List<FarmUser> getFarmUsersByIds(List<Long> farmUserIds);
}
