package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.FarmUserUpdateDTO;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/farm/user")
public class FarmUserController {

    @Resource
    private FarmUserService farmUserService;

    @GetMapping("/info")
    @ApiOperation(value = "获取我的农场用户信息")
    public BaseResponse<FarmUser> getMyFarmUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        FarmUser farmUser = farmUserService.getOrCreateFarmUser(userId);
        return ResultUtils.success(farmUser);
    }

    @PostMapping("/signin")
    @ApiOperation(value = "农场签到")
    public BaseResponse<String> signIn() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        farmUserService.signIn(farmUserId);
        return ResultUtils.success("签到成功，获得5点经验");
    }

    @GetMapping("/signin/status")
    @ApiOperation(value = "获取今日签到状态")
    public BaseResponse<Boolean> getSignInStatus() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        boolean signedToday = farmUserService.isSignedToday(farmUserId);
        return ResultUtils.success(signedToday);
    }

    @GetMapping("/level")
    @ApiOperation(value = "获取农场等级")
    public BaseResponse<Integer> getLevel() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        FarmUser farmUser = farmUserService.getById(farmUserId);
        if (farmUser == null) {
            return ResultUtils.success(1);
        }
        return ResultUtils.success(farmUser.getLevel());
    }

    @GetMapping("/experience")
    @ApiOperation(value = "获取经验值")
    public BaseResponse<Integer> getExperience() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        FarmUser farmUser = farmUserService.getById(farmUserId);
        if (farmUser == null) {
            return ResultUtils.success(0);
        }
        return ResultUtils.success(farmUser.getExperience());
    }

    @GetMapping("/coin")
    @ApiOperation(value = "获取金币数量")
    public BaseResponse<Integer> getCoin() {
        return ResultUtils.success(0);
    }

    @PostMapping("/update-profile")
    @ApiOperation(value = "更新农场用户个人信息")
    public BaseResponse<Boolean> updateProfile(@RequestBody FarmUserUpdateDTO updateDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        boolean result = farmUserService.updateProfile(farmUserId, updateDTO.getNickname(), updateDTO.getAvatar());
        return ResultUtils.success(result);
    }

    @PostMapping("/get-by-ids")
    @ApiOperation(value = "根据农场用户ID批量获取用户信息")
    public BaseResponse<List<FarmUser>> getFarmUsersByIds(@RequestBody List<Long> farmUserIds) {
        List<FarmUser> users = farmUserService.getFarmUsersByIds(farmUserIds);
        return ResultUtils.success(users);
    }
}
