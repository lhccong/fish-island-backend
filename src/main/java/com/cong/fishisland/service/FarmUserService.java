package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.farm.FarmUserVO;
import com.cong.fishisland.model.entity.farm.FarmUser;

import java.util.List;

public interface FarmUserService extends IService<FarmUser> {

    FarmUser getFarmUserByUserId(Long userId);

    FarmUser createFarmUser(Long userId);

    FarmUser getOrCreateFarmUser(Long userId);

    Long getFarmUserId(Long userId);

    FarmUserVO getFarmUserVO(Long systemUserId);

    FarmUserVO toVO(FarmUser farmUser);

    List<FarmUserVO> toVOList(List<FarmUser> farmUsers);

    boolean addExperience(Long userId, Integer exp);

    int calculateLevel(Integer experience);

    boolean incrementTotalHarvest(Long userId);

    boolean incrementTotalSteal(Long userId);

    boolean incrementTotalDefense(Long userId);

    boolean incrementVisitedCount(Long userId);

    boolean incrementFriendCount(Long userId);

    List<FarmUser> getFarmUsersByIds(List<Long> farmUserIds);
}
