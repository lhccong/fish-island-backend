package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmAchievementTaskVO;
import com.cong.fishisland.model.enums.farm.FarmAchievementTypeEnum;

import java.util.List;

/**
 * 农场成就任务服务：里程碑行为驱动，不进入每日任务。
 */
public interface FarmAchievementService {

    /**
     * 进度事件埋点：buff 购买 / buff 升级 / 图鉴解锁 / 农场升级。
     * 内部评估该类型所有启用成就，达成即标记"已完成未领取"。
     *
     * @param targetId 目标对象（buffType 等，可为 null）
     * @param value    事件数值（等级/品级等，可为 null）
     */
    void onProgress(Long userId, FarmAchievementTypeEnum type, String targetId, Integer value);

    /**
     * 我的成就列表（自动刷新一次完成状态，防止遗漏事件）。
     */
    List<FarmAchievementTaskVO> listMy(Long userId);

    /**
     * 领取成就奖励（仅"已完成未领取"可领取）。
     */
    void claim(Long userId, Long taskId);
}
