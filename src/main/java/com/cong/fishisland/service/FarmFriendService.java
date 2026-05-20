
package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;

import java.util.List;

/**
 * 农场互关好友服务（基于 user_follow 互相关注）
 */
public interface FarmFriendService {

    /**
     * 查询互关好友列表，并附带偷菜冷却与是否可偷状态。
     *
     * @param farmUserId   当前访问者的农场用户 ID
     * @param systemUserId 当前访问者的系统用户 ID
     * @return 好友列表 VO
     */
    List<FarmFriendListVO> getFriendsWithStealStatus(Long farmUserId, Long systemUserId);

    /**
     * 统计指定系统用户的互关好友数量。
     *
     * @param systemUserId 系统用户 ID
     * @return 互关好友数
     */
    int getFriendCount(Long systemUserId);

    /**
     * 判断两个系统用户是否互相关注。
     *
     * @param systemUserId       系统用户 ID
     * @param targetSystemUserId 目标系统用户 ID
     * @return true 表示互关
     */
    boolean isMutualFriend(Long systemUserId, Long targetSystemUserId);

    /**
     * 判断当前用户是否可对指定好友农场偷菜（互关、冷却结束且存在可偷作物）。
     *
     * @param farmUserId         当前访问者的农场用户 ID
     * @param friendFarmUserId   好友的农场用户 ID
     * @param systemUserId       当前访问者的系统用户 ID
     * @param targetSystemUserId 好友的系统用户 ID
     * @return true 表示可以偷菜
     */
    boolean canSteal(Long farmUserId, Long friendFarmUserId, Long systemUserId, Long targetSystemUserId);

    /**
     * 访问互关好友的农场，返回地块详情、偷菜状态，并增加好友被访问次数。
     *
     * @param farmUserId         当前访问者的农场用户 ID
     * @param friendFarmUserId   好友的农场用户 ID
     * @param systemUserId       当前访问者的系统用户 ID
     * @param targetSystemUserId 好友的系统用户 ID
     * @return 好友农场详情 VO
     */
    FarmFriendFarmVO visitFriendFarm(Long farmUserId, Long friendFarmUserId, Long systemUserId, Long targetSystemUserId);
}
