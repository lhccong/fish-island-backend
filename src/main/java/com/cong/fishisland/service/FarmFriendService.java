
package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;

import java.util.List;

/**
 * 农场互关好友（基于 user_follow 互相关注）
 */
public interface FarmFriendService {

    List<FarmFriendListVO> getFriendsWithStealStatus(Long farmUserId, Long systemUserId);

    int getFriendCount(Long systemUserId);

    boolean isMutualFriend(Long systemUserId, Long targetSystemUserId);

    boolean canSteal(Long farmUserId, Long friendFarmUserId, Long systemUserId, Long targetSystemUserId);

    FarmFriendFarmVO visitFriendFarm(Long farmUserId, Long friendFarmUserId, Long systemUserId, Long targetSystemUserId);
}
