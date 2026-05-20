
package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.dto.farm.FarmFriendVisitVO;
import com.cong.fishisland.service.FarmFriendService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/farm/friend")
public class FarmFriendController {

    @Resource
    private FarmFriendService farmFriendService;

    @Resource
    private FarmUserService farmUserService;

    @GetMapping("/list")
    @ApiOperation(value = "获取互关好友农场列表（含偷菜状态）")
    public BaseResponse<List<FarmFriendListVO>> getMyFriends() {
        Long systemUserId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(systemUserId);
        List<FarmFriendListVO> friends = farmFriendService.getFriendsWithStealStatus(farmUserId, systemUserId);
        return ResultUtils.success(friends);
    }

    @GetMapping("/count")
    @ApiOperation(value = "获取互关好友数量")
    public BaseResponse<Integer> getFriendCount() {
        Long systemUserId = StpUtil.getLoginIdAsLong();
        int count = farmFriendService.getFriendCount(systemUserId);
        return ResultUtils.success(count);
    }

    @GetMapping("/can-steal")
    @ApiOperation(value = "检查是否可以偷菜（需互相关注）")
    public BaseResponse<Boolean> canSteal(@RequestParam Long friendId) {
        Long systemUserId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(systemUserId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        boolean canSteal = farmFriendService.canSteal(farmUserId, friendFarmUserId, systemUserId, friendId);
        return ResultUtils.success(canSteal);
    }

    @PostMapping("/visit")
    @ApiOperation(value = "访问互关好友农场（含地块详情）")
    public BaseResponse<FarmFriendFarmVO> visitFriendFarm(@RequestParam Long friendId) {
        Long systemUserId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(systemUserId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        FarmFriendFarmVO farmVO = farmFriendService.visitFriendFarm(
                farmUserId, friendFarmUserId, systemUserId, friendId);
        return ResultUtils.success(farmVO);
    }

    @GetMapping("/visit-info")
    @ApiOperation(value = "获取互关好友访问信息（不含地块）")
    public BaseResponse<FarmFriendVisitVO> visitFriend(@RequestParam Long friendId) {
        Long systemUserId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(systemUserId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);

        if (!farmFriendService.isMutualFriend(systemUserId, friendId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "仅可访问互相关注用户的农场");
        }

        FarmFriendVisitVO visitVO = new FarmFriendVisitVO();
        visitVO.setFriendId(friendFarmUserId);
        visitVO.setLastVisitTime(LocalDateTime.now());
        visitVO.setCanSteal(farmFriendService.canSteal(farmUserId, friendFarmUserId, systemUserId, friendId));

        return ResultUtils.success(visitVO);
    }
}
