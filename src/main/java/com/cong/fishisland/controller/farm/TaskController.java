package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.TaskDTO;
import com.cong.fishisland.service.FarmTaskService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private FarmTaskService taskService;

    @Autowired
    private FarmUserService farmUserService;

    @GetMapping("/daily")
    @ApiOperation(value = "获取每日任务列表")
    public BaseResponse<List<TaskDTO>> getDailyTasks() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        return ResultUtils.success(taskService.toDTOList(taskService.getUserTaskRecords(farmUserId)));
    }

    @PostMapping("/claim/{taskId}")
    @ApiOperation(value = "领取任务奖励")
    public BaseResponse<Integer> claimReward(@PathVariable Long taskId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        int exp = taskService.claimTaskReward(farmUserId, taskId);
        if (exp == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(exp);
    }
}
