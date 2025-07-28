package com.cong.fishisland.controller.user;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.user.UserTitleAddRequest;
import com.cong.fishisland.model.dto.user.UserTitleQueryRequest;
import com.cong.fishisland.model.dto.user.UserTitleUpdateRequest;
import com.cong.fishisland.model.entity.user.UserTitle;
import com.cong.fishisland.service.UserTitleService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 称号接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/user/title")
@RequiredArgsConstructor
//@Api(tags = "称号接口")
public class UserTitleController {

    private final UserTitleService userTitleService;

    /**
     * 获取用户可用的称号列表
     *
     * @return {@link BaseResponse}<{@link List}<{@link UserTitle}>>
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取用户可用的称号列表")
    public BaseResponse<List<UserTitle>> listAvailableFrames() {
        return ResultUtils.success(userTitleService.listAvailableTitles());
    }

    /**
     * 设置当前使用的称号
     *
     * @param titleId 称号ID
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/set")
    @ApiOperation(value = "设置当前使用的称号")
    public BaseResponse<Boolean> setCurrentFrame(@RequestParam Long titleId) {
        if (titleId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(userTitleService.setCurrentTitle(titleId));
    }

    /**
     * 根据用户ID查看用户拥有的称号列表（仅管理员）
     *
     * @param userId 用户ID
     * @return {@link BaseResponse}<{@link List}<{@link UserTitle}>>
     */
    @GetMapping("/list/byUserId")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "根据用户ID查看用户拥有的称号列表（仅管理员）")
    public BaseResponse<List<UserTitle>> listUserTitlesByUserId(@RequestParam Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID参数错误");
        }
        return ResultUtils.success(userTitleService.listUserTitlesByUserId(userId));
    }

    /**
     * 给用户添加称号（仅管理员）
     *
     * @param userId 用户ID
     * @param titleId 称号ID
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/add/toUser")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "给用户添加称号（仅管理员）")
    public BaseResponse<Boolean> addTitleToUser(@RequestParam Long userId, @RequestParam Long titleId) {
        if (userId == null || userId <= 0 || titleId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        return ResultUtils.success(userTitleService.addTitleToUser(userId, titleId));
    }

    /**
     * 删除用户称号（仅管理员）
     *
     * @param userId 用户ID
     * @param titleId 称号ID
     * @return {@link BaseResponse}<{@link Boolean}>
     */
    @PostMapping("/remove/fromUser")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "删除用户称号（仅管理员）")
    public BaseResponse<Boolean> removeTitleFromUser(@RequestParam Long userId, @RequestParam Long titleId) {
        if (userId == null || userId <= 0 || titleId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        return ResultUtils.success(userTitleService.removeTitleFromUser(userId, titleId));
    }

    /**
     * 创建称号项（仅管理员）
     *
     * @param userTitleAddRequest 称号添加请求
     * @return 称号ID
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "创建称号项（仅管理员）")
    public BaseResponse<Long> addUserTitle(@RequestBody UserTitleAddRequest userTitleAddRequest) {
        if (userTitleAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String name = userTitleAddRequest.getName();

        if (StringUtils.isBlank(name)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "称号名称不能为空");
        }
        ThrowUtils.throwIf(userTitleService.existTitle(name, null), ErrorCode.OPERATION_ERROR, name+"称号已存在");
        UserTitle userTitle = new UserTitle();
        BeanUtils.copyProperties(userTitleAddRequest, userTitle);

        boolean result = userTitleService.save(userTitle);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(userTitle.getTitleId());
    }

    /**
     * 删除称号项（仅管理员）
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "删除称号项（仅管理员）")
    public BaseResponse<Boolean> deleteUserTitle(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || StringUtils.isBlank(deleteRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = userTitleService.removeById(Long.parseLong(deleteRequest.getId()));
        return ResultUtils.success(result);
    }

    /**
     * 更新称号项（仅管理员）
     *
     * @param userTitleUpdateRequest 称号更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "更新称号项（仅管理员）")
    public BaseResponse<Boolean> updateUserTitle(@RequestBody UserTitleUpdateRequest userTitleUpdateRequest) {
        if (userTitleUpdateRequest == null || userTitleUpdateRequest.getTitleId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = userTitleUpdateRequest.getName();
        ThrowUtils.throwIf(userTitleService.existTitle(name, userTitleUpdateRequest.getTitleId()), ErrorCode.OPERATION_ERROR, name+"称号已存在");
        UserTitle userTitle = new UserTitle();
        BeanUtils.copyProperties(userTitleUpdateRequest, userTitle);

        boolean result = userTitleService.updateById(userTitle);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 根据 ID 获取称号项（仅管理员）
     *
     * @param id 称号ID
     * @return 称号信息
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "根据 ID 获取称号项（仅管理员）")
    public BaseResponse<UserTitle> getUserTitleById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        UserTitle userTitle = userTitleService.getById(id);
        ThrowUtils.throwIf(userTitle == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(userTitle);
    }

    /**
     * 分页获取称号列表（仅管理员）
     *
     * @param userTitleQueryRequest 称号查询请求
     * @return 称号分页列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "分页获取称号列表（仅管理员）")
    public BaseResponse<Page<UserTitle>> listUserTitleByPage(@RequestBody UserTitleQueryRequest userTitleQueryRequest) {
        long current = userTitleQueryRequest.getCurrent();
        long size = userTitleQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "请求页大小不能超过20");

        Page<UserTitle> userTitlePage = userTitleService.page(
                new Page<>(current, size),
                userTitleService.getQueryWrapper(userTitleQueryRequest)
        );

        return ResultUtils.success(userTitlePage);
    }
}