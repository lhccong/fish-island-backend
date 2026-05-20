package com.cong.fishisland.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.farm.FarmDailyTask;
import com.cong.fishisland.model.entity.farm.FarmTaskRecord;

import java.util.List;

public interface FarmTaskService {
    
    List<FarmDailyTask> getAllTasks();
    
    List<FarmTaskRecord> getUserTaskRecords(Long userId);
    
    void updateTaskProgress(Long userId, String taskType);
    
    int claimTaskReward(Long userId, Long taskId);
    
    void initDefaultTasks();
}