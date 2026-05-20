package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.TaskDTO;
import com.cong.fishisland.model.entity.farm.FarmDailyTask;
import com.cong.fishisland.model.entity.farm.FarmTaskRecord;
import com.cong.fishisland.model.enums.farm.FarmTaskTypeEnum;

import java.util.List;

/**
 * 农场每日任务服务
 */
public interface FarmTaskService {

    /**
     * 查询全部每日任务配置（按排序字段升序）。
     *
     * @return 任务配置列表
     */
    List<FarmDailyTask> getAllTasks();

    /**
     * 查询指定用户当日的任务进度记录；若当日无记录则自动初始化。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     * @return 当日任务记录列表
     */
    List<FarmTaskRecord> getUserTaskRecords(Long userId);

    /**
     * 按任务类型更新当日任务进度（如种植、收获、偷菜）。
     *
     * @param userId   农场用户 ID（{@code farm_user.id}）
     * @param taskType 任务类型
     */
    void updateTaskProgress(Long userId, FarmTaskTypeEnum taskType);

    /**
     * 领取已完成任务的奖励。
     *
     * @param userId 农场用户 ID（{@code farm_user.id}）
     * @param taskId 任务配置 ID
     * @return 领取的奖励积分；失败或不可领取时返回 0
     */
    int claimTaskReward(Long userId, Long taskId);

    /**
     * 初始化系统默认的每日任务配置（管理端或启动时调用）。
     */
    void initDefaultTasks();

    /**
     * 将任务进度记录转换为 DTO。
     *
     * @param record 任务进度记录
     * @return 任务 DTO；入参为 null 时返回 null
     */
    TaskDTO toDTO(FarmTaskRecord record);

    /**
     * 批量将任务进度记录转换为 DTO。
     *
     * @param records 任务进度记录列表
     * @return 任务 DTO 列表
     */
    List<TaskDTO> toDTOList(List<FarmTaskRecord> records);
}
