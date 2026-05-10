package com.cong.fishisland.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.user.UserFollowPageRequest;
import com.cong.fishisland.model.dto.user.UserFollowRequest;
import com.cong.fishisland.model.vo.user.UserFollowVO;
import com.cong.fishisland.service.UserFollowService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户关注接口
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@RestController
@RequestMapping("/follow")
@Slf4j
public class UserFollowController {

    @Resource
    private UserFollowService userFollowService;

    /**
     * 关注 / 取消关注
     * <p>
     * 已关注则取消，未关注则新增，幂等操作。
     */
    @GetMapping("/toggle")
    @ApiOperation(value = "关注/取消关注", notes = "已关注则取消，未关注则新增")
    public BaseResponse<Boolean> toggleFollow(UserFollowRequest request) {
        if (request == null || request.getFollowUserId() == null || request.getFollowUserId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        boolean result = userFollowService.toggleFollow(request.getFollowUserId());
        return ResultUtils.success(result);
    }

    /**
     * 查询我的关注列表（分页）
     */
    @GetMapping("/following")
    @ApiOperation(value = "查询我的关注列表", notes = "分页返回当前用户关注的人")
    public BaseResponse<Page<UserFollowVO>> listMyFollowing(UserFollowPageRequest request) {
        if (request == null) {
            request = new UserFollowPageRequest();
        }
        if (request.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页大小不能超过50");
        }
        return ResultUtils.success(userFollowService.listMyFollowing(request.getCurrent(), request.getPageSize()));
    }

    /**
     * 查询我的粉丝列表（分页）
     */
    @GetMapping("/followers")
    @ApiOperation(value = "查询我的粉丝列表", notes = "分页返回关注当前用户的人")
    public BaseResponse<Page<UserFollowVO>> listMyFollowers(UserFollowPageRequest request) {
        if (request == null) {
            request = new UserFollowPageRequest();
        }
        if (request.getPageSize() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页大小不能超过50");
        }
        return ResultUtils.success(userFollowService.listMyFollowers(request.getCurrent(), request.getPageSize()));
    }

    /**
     * 判断是否已关注某用户
     */
    @GetMapping("/is-following")
    @ApiOperation(value = "是否已关注", notes = "判断当前用户是否已关注指定用户")
    public BaseResponse<Boolean> isFollowing(UserFollowRequest request) {
        if (request == null || request.getFollowUserId() == null || request.getFollowUserId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        return ResultUtils.success(userFollowService.isFollowing(request.getFollowUserId()));
    }
}
