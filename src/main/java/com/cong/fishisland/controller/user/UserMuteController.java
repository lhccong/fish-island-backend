package com.cong.fishisland.controller.user;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.user.UserMuteRequest;
import com.cong.fishisland.model.vo.user.UserMuteVO;
import com.cong.fishisland.service.UserMuteService;
import com.cong.fishisland.service.UserVipService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户禁言控制器
 *
 * @author cong
 */
@RestController
@RequestMapping("/user/mute")
@Slf4j
//@Api(tags = "用户禁言相关")
public class UserMuteController {

    @Resource
    private UserMuteService userMuteService;
    @Resource
    private UserVipService userVipService;

    /**
     * 禁言用户
     *
     * @param userMuteRequest 用户禁言请求
     * @return 操作结果
     */
    @PostMapping("/add")
    @ApiOperation(value = "禁言用户")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> muteUser(@RequestBody UserMuteRequest userMuteRequest) {
        if (userMuteRequest == null || userMuteRequest.getUserId() == null || userMuteRequest.getDuration() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (userMuteRequest.getDuration() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁言时间必须大于0");
        }
        if (userVipService.isUserVip(userMuteRequest.getUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "VIP用户不能被禁言");
        }
        boolean result = userMuteService.muteUser(userMuteRequest.getUserId(), userMuteRequest.getDuration());
        return ResultUtils.success(result);
    }

    /**
     * 解除用户禁言
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/remove")
    @ApiOperation(value = "解除用户禁言")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> unmuteUser(@RequestParam Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        boolean result = userMuteService.unmuteUser(userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户禁言状态
     *
     * @param userId 用户ID
     * @return 用户禁言信息
     */
    @GetMapping("/info")
    @ApiOperation(value = "获取用户禁言状态")
    public BaseResponse<UserMuteVO> getUserMuteInfo(@RequestParam Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        UserMuteVO userMuteInfo = userMuteService.getUserMuteInfo(userId);
        return ResultUtils.success(userMuteInfo);
    }
} 