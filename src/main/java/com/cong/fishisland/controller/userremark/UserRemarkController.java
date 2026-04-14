package com.cong.fishisland.controller.userremark;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.userremark.UserRemarkAddRequest;
import com.cong.fishisland.model.entity.userremark.UserRemark;
import com.cong.fishisland.service.UserRemarkService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户备注控制器
 * @author cong
 */
@RestController
@RequestMapping("/userRemark")
@Slf4j
@RequiredArgsConstructor
public class UserRemarkController {

    private final UserRemarkService userRemarkService;

    /**
     * 保存备注
     */
    @PostMapping("/save")
    @ApiOperation(value = "保存备注")
    public BaseResponse<Boolean> saveRemark(@RequestBody UserRemarkAddRequest request) {
        if (!StpUtil.isLogin()) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        return ResultUtils.success(userRemarkService.saveRemark(request.getContent()));
    }

    /**
     * 获取当前用户备注
     */
    @GetMapping("/get")
    @ApiOperation(value = "获取当前用户备注")
    public BaseResponse<UserRemark> getRemark() {
        if (!StpUtil.isLogin()) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        return ResultUtils.success(userRemarkService.getCurrentUserRemark());
    }
}
