package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.service.FarmFriendService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 农场好友（互相关注用户）
 */
@RestController
@RequestMapping("/farm/friend")
public class FarmFriendController {

    @Resource
    private FarmFriendService farmFriendService;

    @GetMapping("/list")
    @ApiOperation(value = "获取农场好友列表（互相关注）")
    public BaseResponse<List<FarmFriendListVO>> listFriends() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(farmFriendService.getFriendsWithStealStatus(userId));
    }

    @GetMapping("/lands")
    @ApiOperation(value = "获取好友地块列表（与我的地块数据结构一致）")
    public BaseResponse<List<LandDTO>> getFriendLands(@RequestParam Long friendUserId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(farmFriendService.getFriendLands(userId, friendUserId));
    }
}
