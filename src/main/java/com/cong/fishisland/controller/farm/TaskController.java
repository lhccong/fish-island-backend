package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.mapper.farm.FarmDailyTaskMapper;
import com.cong.fishisland.model.dto.farm.TaskDTO;
import com.cong.fishisland.model.entity.farm.FarmDailyTask;
import com.cong.fishisland.model.entity.farm.FarmTaskRecord;
import com.cong.fishisland.service.FarmTaskService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private FarmTaskService taskService;

    @Autowired
    private FarmDailyTaskMapper dailyTaskMapper;

    @Autowired
    private FarmUserService farmUserService;

    @GetMapping("/daily")
    @ApiOperation(value = "获取每日任务列表")
    public BaseResponse<List<TaskDTO>> getDailyTasks() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        List<FarmTaskRecord> records = taskService.getUserTaskRecords(farmUserId);
        List<TaskDTO> dtos = records.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResultUtils.success(dtos);
    }

    @PostMapping("/claim/{taskId}")
    @ApiOperation(value = "领取任务奖励")
    public BaseResponse<Integer> claimReward(@PathVariable Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        int exp = taskService.claimTaskReward(farmUserId, taskId);
        if (exp == 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(exp);
    }

    private TaskDTO convertToDTO(FarmTaskRecord record) {
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
}