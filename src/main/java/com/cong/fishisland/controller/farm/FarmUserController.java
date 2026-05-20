package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.FarmUserVO;
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
    public BaseResponse<FarmUserVO> getMyFarmUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(farmUserService.getFarmUserVO(userId));
    }

    @PostMapping("/get-by-ids")
    @ApiOperation(value = "根据系统用户ID批量获取农场用户信息")
    public BaseResponse<List<FarmUserVO>> getFarmUsersByUserIds(@RequestBody List<Long> userIds) {
        List<FarmUser> users = farmUserService.getFarmUsersByUserIds(userIds);
        return ResultUtils.success(farmUserService.toVOList(users));
    }
}
