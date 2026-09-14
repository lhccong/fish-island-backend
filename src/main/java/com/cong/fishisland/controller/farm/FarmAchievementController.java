package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.FarmAchievementTaskVO;
import com.cong.fishisland.model.dto.farm.request.FarmAchievementClaimRequest;
import com.cong.fishisland.service.FarmAchievementService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/achievement")
public class FarmAchievementController {

    @Resource
    private FarmAchievementService farmAchievementService;

    @GetMapping("/list")
    @ApiOperation(value = "我的农场成就任务", notes = "里程碑成就（buff升级/购买、图鉴品级、农场等级），非每日任务")
    public BaseResponse<List<FarmAchievementTaskVO>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(farmAchievementService.listMy(userId));
    }

    @PostMapping("/claim")
    @ApiOperation(value = "领取成就奖励", notes = "仅'已完成未领取'状态可领取，防重复领取")
    public BaseResponse<Boolean> claim(@RequestBody FarmAchievementClaimRequest request) {
        if (request == null || request.getTaskId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        farmAchievementService.claim(userId, request.getTaskId());
        return ResultUtils.success(true);
    }
}
