
package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.entity.farm.FarmFriend;

import java.time.LocalDateTime;
import java.util.List;
/**
 * @description:
 * @author: xiayuchen
 * @date: 2026/5/13 15:25
 * @param:
 * @return:
 **/
public interface FarmFriendService {

    List<FarmFriend> getFriendsByUserId(Long userId);

    List<FarmFriendListVO> getFriendsWithStealStatus(Long userId);

    FarmFriend getFriend(Long userId, Long friendId);

    int getFriendCount(Long userId);

    FarmFriend addFriend(Long userId, Long friendId);

    boolean removeFriend(Long userId, Long friendId);

    boolean blockFriend(Long userId, Long friendId);

    boolean unblockFriend(Long userId, Long friendId);

    boolean updateLastVisitTime(Long userId, Long friendId);

    boolean updateStealCooldown(Long userId, Long friendId, LocalDateTime cooldownTime);

    boolean canSteal(Long userId, Long friendId);

    List<FarmFriend> getActiveFriends(Long userId);

    FarmFriendFarmVO visitFriendFarm(Long userId, Long friendId);
}