package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import org.springframework.transaction.annotation.Transactional;

import com.cong.fishisland.model.entity.farm.FarmLand;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 农场偷菜服务
 */
public interface FarmStealService {

    /**
     * 偷取指定种植记录上的成熟作物，发放积分并更新排行榜与任务进度。
     *
     * @param stealerId 偷菜者的系统用户 ID
     * @param landId    地块 ID
     * @return 偷菜记录
     */
    FarmStealRecord steal(Long stealerId, Long landId);

    /**
     * 判断指定偷菜者是否可偷该地块（每人每地块当前作物仅可偷一次）。
     */
    boolean canStealLand(Long stealerId, Long landId);

    /**
     * 批量判断地块是否可偷（调用方需已校验互关好友，本方法不再查关注表）。
     *
     * @return key 为地块 ID，value 为是否可偷
     */
    Map<Long, Boolean> batchCanStealLand(Long stealerId, List<FarmLand> lands);

    /**
     * 查询当前用户在本轮作物中已偷过的地块 ID 集合。
     */
    Set<Long> findStolenLandIdsForCurrentCrop(Long stealerId, Collection<Long> landIds);

    /**
     * 校验偷菜者与农场主是否为互关好友。
     *
     * @param stealerId 偷菜者的系统用户 ID
     * @param ownerId   农场主的系统用户 ID
     * @return true 表示互关
     */
    boolean validateFriend(Long stealerId, Long ownerId);

    /**
     * 更新偷菜相关的每日任务进度。
     *
     * @param stealerId 偷菜者的系统用户 ID
     */
    void updateTaskProgress(Long stealerId);

    /**
     * 查询指定用户作为偷菜者的偷菜记录。
     *
     * @param stealerId 偷菜者的系统用户 ID
     * @return 偷菜记录列表
     */
    List<FarmStealRecord> getStealRecordsByStealer(Long stealerId);

    /**
     * 查询指定用户作为被偷者的偷菜记录（含偷菜者昵称等信息）。
     *
     * @param ownerId 农场主的系统用户 ID
     * @return 偷菜记录 VO 列表
     */
    List<FarmStealRecordVO> getStealRecordsByOwner(Long ownerId);
}
