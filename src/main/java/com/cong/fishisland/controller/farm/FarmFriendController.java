
package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.FarmFriendFarmVO;
import com.cong.fishisland.model.dto.farm.FarmFriendListVO;
import com.cong.fishisland.model.dto.farm.FarmFriendVisitVO;
import com.cong.fishisland.model.entity.farm.FarmFriend;
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

    /**
     * @description: 获取我的好友列表
     * @author: xiayuchen
     * @date: 2026/5/13 15:27
     * @param: []
     * @return: com.cong.fishisland.common.BaseResponse<java.util.List < com.cong.fishisland.model.dto.farm.FarmFriendListVO>>
     **/
    @GetMapping("/list")
    @ApiOperation(value = "获取我的农场好友列表")
    public BaseResponse<List<FarmFriendListVO>> getMyFriends() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        List<FarmFriendListVO> friends = farmFriendService.getFriendsWithStealStatus(farmUserId);
        return ResultUtils.success(friends);
    }

    @GetMapping("/count")
    @ApiOperation(value = "获取好友数量")
    public BaseResponse<Integer> getFriendCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        int count = farmFriendService.getFriendCount(farmUserId);
        return ResultUtils.success(count);
    }

    @PostMapping("/add")
    @ApiOperation(value = "添加好友")
    public BaseResponse<FarmFriend> addFriend(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (userId.equals(friendId)) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "不能添加自己为好友");
        }
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        FarmFriend friend = farmFriendService.addFriend(farmUserId, friendFarmUserId);
        return ResultUtils.success(friend);
    }

    @PostMapping("/remove")
    @ApiOperation(value = "删除好友")
    public BaseResponse<Boolean> removeFriend(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        boolean result = farmFriendService.removeFriend(farmUserId, friendFarmUserId);
        return ResultUtils.success(result);
    }

    @PostMapping("/block")
    @ApiOperation(value = "拉黑好友（不可偷菜）")
    public BaseResponse<Boolean> blockFriend(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        boolean result = farmFriendService.blockFriend(farmUserId, friendFarmUserId);
        return ResultUtils.success(result);
    }

    @PostMapping("/unblock")
    @ApiOperation(value = "解封好友")
    public BaseResponse<Boolean> unblockFriend(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        boolean result = farmFriendService.unblockFriend(farmUserId, friendFarmUserId);
        return ResultUtils.success(result);
    }

    @GetMapping("/can-steal")
    @ApiOperation(value = "检查是否可以偷菜")
    public BaseResponse<Boolean> canSteal(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);
        boolean canSteal = farmFriendService.canSteal(farmUserId, friendFarmUserId);
        return ResultUtils.success(canSteal);
    }

    @PostMapping("/visit")
    @ApiOperation(value = "访问好友农场，完整的农场信息（包括地块详情）")
    public BaseResponse<FarmFriendFarmVO> visitFriendFarm(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        FarmFriendFarmVO farmVO = farmFriendService.visitFriendFarm(farmUserId, friendId);
        return ResultUtils.success(farmVO);
    }

    @GetMapping("/visit-info")
    @ApiOperation(value = "获取好友访问信息，基础访问信息（不包含地块),用于快速检查访问状态和偷菜权限")
    public BaseResponse<FarmFriendVisitVO> visitFriend(@RequestParam Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Long friendFarmUserId = farmUserService.getFarmUserId(friendId);

        FarmFriend friend = farmFriendService.getFriend(farmUserId, friendFarmUserId);
        if (friend == null || friend.getStatus() == 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "好友关系不存在或已被拉黑");
        }

        farmFriendService.updateLastVisitTime(farmUserId, friendFarmUserId);

        FarmFriendVisitVO visitVO = new FarmFriendVisitVO();
        visitVO.setFriendId(friendFarmUserId);
        visitVO.setLastVisitTime(LocalDateTime.now());
        visitVO.setCanSteal(farmFriendService.canSteal(farmUserId, friendFarmUserId));

        return ResultUtils.success(visitVO);
    }
}