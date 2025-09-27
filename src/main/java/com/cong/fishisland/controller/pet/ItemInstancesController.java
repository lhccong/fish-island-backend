package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.dto.pet.ItemInstanceQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 物品实例接口
 *
 * @author Shing
 * date 26/9/2025 星期五
 */
@RestController
@RequestMapping("/item")
public class ItemInstancesController {

    @Resource
    private ItemInstancesService itemInstancesService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 分页获取当前用户物品列表（封装类）
     *
     * @param itemInstanceQueryRequest 分页查询请求
     * @return 物品列表分页
     */
    @PostMapping("/my/list/page/vo")
    @ApiOperation(value = "分页获取当前用户的物品列表（封装类）")
    @SaCheckLogin
    public BaseResponse<Page<ItemInstanceVO>> listMyItemInstancesByPage(@RequestBody ItemInstanceQueryRequest itemInstanceQueryRequest) {
        if (itemInstanceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        long current = itemInstanceQueryRequest.getCurrent();
        long pageSize = itemInstanceQueryRequest.getPageSize();

        // 限制单页最大数量，防止爬虫或恶意请求
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "单页最多查询50条");

        // 1. 构造查询条件
        QueryWrapper<ItemInstances> queryWrapper = itemInstancesService.getQueryWrapper(itemInstanceQueryRequest);
        // 2. 获取当前用户物品实例
        User loginUser = userService.getLoginUser();
        queryWrapper.eq("ownerUserId", loginUser.getId());
        // 3. 分页查询物品实例
        Page<ItemInstances> itemInstancesPage = itemInstancesService.page(new Page<>(current, pageSize), queryWrapper);
        // 3. 转换 VO 并返回
        Page<ItemInstanceVO> itemInstancesVoPage = itemInstancesService.getItemInstancesVoPage(itemInstancesPage);
        return ResultUtils.success(itemInstancesVoPage);

    }

    /**
     * 分页获取物品列表（封装类）
     *
     * @param itemInstanceQueryRequest 分页查询请求
     * @return 物品列表分页
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取物品列表（封装类）")
    @SaCheckLogin
    public BaseResponse<Page<ItemInstanceVO>> listItemInstancesVOByPage(@RequestBody ItemInstanceQueryRequest itemInstanceQueryRequest) {
        if (itemInstanceQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        long current = itemInstanceQueryRequest.getCurrent();
        long pageSize = itemInstanceQueryRequest.getPageSize();

        // 限制单页最大数量，防止爬虫或恶意请求
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "单页最多查询50条");

        // 1. 构造查询条件
        QueryWrapper<ItemInstances> queryWrapper = itemInstancesService.getQueryWrapper(itemInstanceQueryRequest);
        // 2. 分页查询物品实例
        Page<ItemInstances> itemInstancesPage = itemInstancesService.page(new Page<>(current, pageSize), queryWrapper);
        // 3. 转换 VO 并返回
        Page<ItemInstanceVO> itemInstancesVoPage = itemInstancesService.getItemInstancesVoPage(itemInstancesPage);
        return ResultUtils.success(itemInstancesVoPage);

    }

    // endregion

}
