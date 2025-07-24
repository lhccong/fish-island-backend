package com.cong.fishisland.controller.user;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.user.UserVipAddRequest;
import com.cong.fishisland.model.dto.user.UserVipQueryRequest;
import com.cong.fishisland.model.dto.user.UserVipUpdateRequest;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.model.vo.user.UserVipVO;
import com.cong.fishisland.service.UserVipService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户会员接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/user/vip")
@Slf4j
//@Api(tags = "用户会员接口")
public class UserVipController {

    @Resource
    private UserVipService userVipService;

    /**
     * 创建会员
     *
     * @param userVipAddRequest 会员创建请求
     * @return {@link BaseResponse}<{@link Long}> 会员id
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "创建会员（仅管理员）")
    public BaseResponse<Long> addUserVip(@RequestBody UserVipAddRequest userVipAddRequest) {
        ThrowUtils.throwIf(userVipAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long userVipId = userVipService.createVip(userVipAddRequest);
        return ResultUtils.success(userVipId);
    }

    /**
     * 删除会员
     *
     * @param deleteRequest 删除请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "删除会员（仅管理员）")
    public BaseResponse<Boolean> deleteUserVip(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userVipService.removeById(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(b);
    }

    /**
     * 更新会员
     *
     * @param userVipUpdateRequest 会员更新请求
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "更新会员（仅管理员）")
    public BaseResponse<Boolean> updateUserVip(@RequestBody UserVipUpdateRequest userVipUpdateRequest) {
        ThrowUtils.throwIf(userVipUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userVipService.updateVip(userVipUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取会员
     *
     * @param id 会员id
     * @return {@link BaseResponse}<{@link UserVipVO}>
     */
    @GetMapping("/get/vo")
    @ApiOperation(value = "根据id获取会员信息")
    public BaseResponse<UserVipVO> getUserVipVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVip userVip = userVipService.getById(id);
        if (userVip == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(userVipService.getVipVO(userVip));
    }

    /**
     * 获取当前登录用户的会员信息
     *
     * @return {@link BaseResponse}<{@link UserVipVO}>
     */
    @GetMapping("/get/my")
    @ApiOperation(value = "获取当前登录用户的会员信息")
    public BaseResponse<UserVipVO> getCurrentUserVip() {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        // 查询用户会员信息
        UserVip userVip = userVipService.lambdaQuery()
                .eq(UserVip::getUserId, userId)
                .eq(UserVip::getIsDelete, 0)
                .one();
        
        // 如果用户不是会员，返回空
        if (userVip == null) {
            return ResultUtils.success(null);
        }
        
        return ResultUtils.success(userVipService.getVipVO(userVip));
    }

    /**
     * 检查当前用户是否是会员
     *
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @GetMapping("/check")
    @ApiOperation(value = "检查当前用户是否是会员")
    public BaseResponse<Boolean> checkUserVip() {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isVip = userVipService.isUserVip(userId);
        return ResultUtils.success(isVip);
    }

    /**
     * 检查当前用户是否是永久会员
     *
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @GetMapping("/check/permanent")
    @ApiOperation(value = "检查当前用户是否是永久会员")
    public BaseResponse<Boolean> checkPermanentVip() {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isPermanentVip = userVipService.isPermanentVip(userId);
        return ResultUtils.success(isPermanentVip);
    }

    /**
     * 分页获取会员列表（仅管理员）
     *
     * @param userVipQueryRequest 会员查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link UserVip}>>
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取会员列表（仅管理员）")
    public BaseResponse<Page<UserVip>> listUserVipByPage(@RequestBody UserVipQueryRequest userVipQueryRequest) {
        long current = userVipQueryRequest.getCurrent();
        long size = userVipQueryRequest.getPageSize();
        Page<UserVip> userVipPage = userVipService.page(new Page<>(current, size),
                userVipService.getQueryWrapper(userVipQueryRequest));
        return ResultUtils.success(userVipPage);
    }

    /**
     * 分页获取会员列表（封装类）（仅管理员）
     *
     * @param userVipQueryRequest 会员查询请求
     * @return {@link BaseResponse}<{@link Page}<{@link UserVipVO}>>
     */
    @PostMapping("/list/page/vo")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取会员列表（封装类）（仅管理员）")
    public BaseResponse<Page<UserVipVO>> listUserVipVOByPage(@RequestBody UserVipQueryRequest userVipQueryRequest) {
        if (userVipQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = userVipQueryRequest.getCurrent();
        long size = userVipQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<UserVip> userVipPage = userVipService.page(new Page<>(current, size),
                userVipService.getQueryWrapper(userVipQueryRequest));
        Page<UserVipVO> userVipVOPage = userVipService.getVipVOPage(userVipPage);
        return ResultUtils.success(userVipVOPage);
    }
} 