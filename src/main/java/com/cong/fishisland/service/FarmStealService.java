package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 农场偷菜服务
 */
public interface FarmStealService {

    /**
     * 偷取指定种植记录上的成熟作物，发放积分并更新排行榜与任务进度。
     *
     * @param stealerId     偷菜者的农场用户 ID
     * @param plantRecordId 种植记录 ID
     * @return 偷菜记录
     */
    @Transactional
    FarmStealRecord steal(Long stealerId, Long plantRecordId);

    /**
     * 检查偷菜冷却是否已结束（同一偷菜者对同一农场主，冷却 10 分钟）。
     *
     * @param stealerId     偷菜者的农场用户 ID
     * @param plantRecordId 种植记录 ID（用于定位农场主）
     * @return true 表示可以偷（冷却已结束）
     */
    boolean checkCooldown(Long stealerId, Long plantRecordId);

    /**
     * 校验偷菜者与农场主是否为互关好友。
     *
     * @param stealerId 偷菜者的农场用户 ID
     * @param ownerId   农场主的农场用户 ID
     * @return true 表示互关
     */
    boolean validateFriend(Long stealerId, Long ownerId);

    /**
     * 更新偷菜相关的每日任务进度。
     *
     * @param stealerId 偷菜者的农场用户 ID
     */
    void updateTaskProgress(Long stealerId);

    /**
     * 查询指定用户作为偷菜者的偷菜记录。
     *
     * @param stealerId 偷菜者的农场用户 ID
     * @return 偷菜记录列表
     */
    List<FarmStealRecord> getStealRecordsByStealer(Long stealerId);

    /**
     * 查询指定用户作为被偷者的偷菜记录（含偷菜者昵称等信息）。
     *
     * @param ownerId 农场主的农场用户 ID
     * @return 偷菜记录 VO 列表
     */
    List<FarmStealRecordVO> getStealRecordsByOwner(Long ownerId);
}
