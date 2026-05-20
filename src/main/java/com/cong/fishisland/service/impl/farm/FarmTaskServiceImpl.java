package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmDailyTaskMapper;
import com.cong.fishisland.mapper.farm.FarmTaskRecordMapper;
import com.cong.fishisland.model.dto.farm.TaskDTO;
import com.cong.fishisland.model.entity.farm.FarmDailyTask;
import com.cong.fishisland.model.entity.farm.FarmTaskRecord;
import com.cong.fishisland.service.FarmTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FarmTaskServiceImpl extends ServiceImpl<FarmTaskRecordMapper, FarmTaskRecord> implements FarmTaskService {

    @Autowired
    private FarmDailyTaskMapper dailyTaskMapper;

    @Override
    public List<FarmDailyTask> getAllTasks() {
        return dailyTaskMapper.selectList(new LambdaQueryWrapper<FarmDailyTask>()
                .orderByAsc(FarmDailyTask::getSortOrder));
    }

    @Override
    public List<FarmTaskRecord> getUserTaskRecords(Long userId) {
        LocalDate today = LocalDate.now();
        List<FarmTaskRecord> records = list(new LambdaQueryWrapper<FarmTaskRecord>()
                .eq(FarmTaskRecord::getUserId, userId)
                .eq(FarmTaskRecord::getDate, today));

        if (records.isEmpty()) {
            initDailyTasks(userId);
            records = list(new LambdaQueryWrapper<FarmTaskRecord>()
                    .eq(FarmTaskRecord::getUserId, userId)
                    .eq(FarmTaskRecord::getDate, today));
        }
        return records;
    }

    private void initDailyTasks(Long userId) {
        List<FarmDailyTask> tasks = dailyTaskMapper.selectList(new LambdaQueryWrapper<FarmDailyTask>()
                .orderByAsc(FarmDailyTask::getSortOrder));
        if (tasks.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<FarmTaskRecord> records = new ArrayList<>();
        for (FarmDailyTask task : tasks) {
            FarmTaskRecord record = new FarmTaskRecord();
            record.setUserId(userId);
            record.setTaskId(task.getId());
            record.setCurrentCount(0);
            record.setCompleted(0);
            record.setClaimed(0);
            record.setDate(today);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            records.add(record);
        }
        saveBatch(records);
    }

    @Override
    public void updateTaskProgress(Long userId, String taskType) {
        List<FarmDailyTask> tasks = dailyTaskMapper.selectList(new LambdaQueryWrapper<FarmDailyTask>()
                .eq(FarmDailyTask::getType, taskType));
        if (tasks.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<Long> taskIds = tasks.stream().map(FarmDailyTask::getId).collect(Collectors.toList());
        List<FarmTaskRecord> records = list(new LambdaQueryWrapper<FarmTaskRecord>()
                .eq(FarmTaskRecord::getUserId, userId)
                .in(FarmTaskRecord::getTaskId, taskIds)
                .eq(FarmTaskRecord::getDate, today));

        Map<Long, Integer> targetMap = tasks.stream()
                .collect(Collectors.toMap(FarmDailyTask::getId, FarmDailyTask::getTargetCount));

        List<FarmTaskRecord> toUpdate = new ArrayList<>();
        for (FarmTaskRecord record : records) {
            if (record.getCompleted() == 0) {
                record.setCurrentCount(record.getCurrentCount() + 1);
                Integer target = targetMap.get(record.getTaskId());
                if (target != null && record.getCurrentCount() >= target) {
                    record.setCompleted(1);
                }
                record.setUpdateTime(LocalDateTime.now());
                toUpdate.add(record);
            }
        }
        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }
    }

    @Override
    public int claimTaskReward(Long userId, Long taskId) {
        LocalDate today = LocalDate.now();
        FarmTaskRecord record = getOne(new LambdaQueryWrapper<FarmTaskRecord>()
                .eq(FarmTaskRecord::getUserId, userId)
                .eq(FarmTaskRecord::getTaskId, taskId)
                .eq(FarmTaskRecord::getDate, today)
                .last("LIMIT 1"));

        if (record == null || record.getCompleted() == 0 || record.getClaimed() == 1) {
            return 0;
        }

        FarmDailyTask task = dailyTaskMapper.selectById(taskId);
        if (task == null) {
            return 0;
        }

        record.setClaimed(1);
        record.setUpdateTime(LocalDateTime.now());
        updateById(record);

        return task.getRewardExp();
    }

    @Override
    public void initDefaultTasks() {
        if (dailyTaskMapper.selectCount(null) == 0) {
            createTask("收获3次", "收获作物3次", 3, 10, "harvest", 1);
            createTask("照料1次", "照料作物1次", 1, 5, "replant", 2);
            createTask("种植3种不同作物", "种植3种不同的作物", 3, 8, "plant", 3);
            createTask("每日访问农场", "访问好友农场", 1, 3, "visit", 4);
        }
    }

    private void createTask(String name, String desc, int target, int reward, String type, int order) {
        FarmDailyTask task = new FarmDailyTask();
        task.setName(name);
        task.setDescription(desc);
        task.setTargetCount(target);
        task.setRewardExp(reward);
        task.setType(type);
        task.setSortOrder(order);
        task.setCreateTime(LocalDateTime.now());
        dailyTaskMapper.insert(task);
    }

    @Override
    public TaskDTO toDTO(FarmTaskRecord record) {
        if (record == null) {
            return null;
        }
        TaskDTO dto = new TaskDTO();
        dto.setId(record.getTaskId());
        dto.setCurrentCount(record.getCurrentCount());
        dto.setCompleted(record.getCompleted());
        dto.setClaimed(record.getClaimed());

        FarmDailyTask task = dailyTaskMapper.selectById(record.getTaskId());
        if (task != null) {
            dto.setName(task.getName());
            dto.setDescription(task.getDescription());
            dto.setTargetCount(task.getTargetCount());
            dto.setRewardExp(task.getRewardExp());
            dto.setType(task.getType());
        }
        return dto;
    }

    @Override
    public List<TaskDTO> toDTOList(List<FarmTaskRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> taskIds = records.stream()
                .map(FarmTaskRecord::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, FarmDailyTask> taskMap = taskIds.isEmpty()
                ? Collections.emptyMap()
                : dailyTaskMapper.selectBatchIds(taskIds).stream()
                        .collect(Collectors.toMap(FarmDailyTask::getId, Function.identity()));

        return records.stream().map(record -> {
            TaskDTO dto = new TaskDTO();
            dto.setId(record.getTaskId());
            dto.setCurrentCount(record.getCurrentCount());
            dto.setCompleted(record.getCompleted());
            dto.setClaimed(record.getClaimed());
            FarmDailyTask task = taskMap.get(record.getTaskId());
            if (task != null) {
                dto.setName(task.getName());
                dto.setDescription(task.getDescription());
                dto.setTargetCount(task.getTargetCount());
                dto.setRewardExp(task.getRewardExp());
                dto.setType(task.getType());
            }
            return dto;
        }).collect(Collectors.toList());
    }
}
