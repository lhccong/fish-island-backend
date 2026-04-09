package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.item.ItemEquipRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceAddRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceDecomposeRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceEditRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceQueryRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceUpdateRequest;
import com.cong.fishisland.model.dto.item.ItemUnequipRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 物品实例接口
 *
 * @author Shing
 * date 26/9/2025 星期五
 */
@RestController
@RequestMapping("/itemInstances")
@Validated
public class ItemInstancesController {

    @Resource
    private ItemInstancesService itemInstancesService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 添加物品实例
     */
    @PostMapping("/add")
    @SaCheckLogin
    @ApiOperation("添加物品")
    public BaseResponse<Long> addItemInstance(@RequestBody @Validated ItemInstanceAddRequest itemInstanceAddRequest) {
        ThrowUtils.throwIf(itemInstanceAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 调用 Service 创建物品实例
        Long itemInstanceId = itemInstancesService.addItemInstance(itemInstanceAddRequest);
        return ResultUtils.success(itemInstanceId);
    }

    /**
     * 用户更新自己持有的物品
     */
    @PostMapping("/update")
    @SaCheckLogin
    @ApiOperation("更新物品信息（用户）")
    public BaseResponse<Boolean> updateItemInstance(@RequestBody @Validated ItemInstanceUpdateRequest itemInstanceUpdateRequest) {
        if (itemInstanceUpdateRequest == null || itemInstanceUpdateRequest.getTemplateId() == null || itemInstanceUpdateRequest.getTemplateId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean success = itemInstancesService.updateItemInstance(itemInstanceUpdateRequest);
        return ResultUtils.success(success);
    }

    /**
     * 用户删除自己持有的物品
     */
    @PostMapping("/delete")
    @SaCheckLogin
    @ApiOperation("删除物品")
    public BaseResponse<Boolean> deleteItemInstance(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || Long.parseLong(deleteRequest.getId()) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = Long.parseLong(deleteRequest.getId());
        // 判断是否存在
        ItemInstances oldItemInstance = itemInstancesService.getById(id);
        ThrowUtils.throwIf(oldItemInstance == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldItemInstance.getOwnerUserId().equals(user.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = itemInstancesService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    /**
     * 分解物品实例，获得积分
     */
    @PostMapping("/decompose")
    @SaCheckLogin
    @ApiOperation("分解物品")
    public BaseResponse<Long> decomposeItemInstance(@RequestBody @Validated ItemInstanceDecomposeRequest decomposeRequest) {
        if (decomposeRequest == null || decomposeRequest.getItemInstanceId() == null || decomposeRequest.getItemInstanceId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品实例ID不能为空");
        }
        Long totalPoints = itemInstancesService.decomposeItemInstance(decomposeRequest.getItemInstanceId(), null, null, null);
        return ResultUtils.success(totalPoints);
    }

    /**
     * 批量分解蓝绿装备（稀有度1、2）
     * 已穿戴的装备不会被分解
     */
    @PostMapping("/batchDecomposeBlueGreen")
    @SaCheckLogin
    @ApiOperation("批量分解蓝绿装备")
    public BaseResponse<Long> batchDecomposeBlueGreenEquipments() {
        Long totalPoints = itemInstancesService.batchDecomposeBlueGreenEquipments();
        return ResultUtils.success(totalPoints);
    }

    /**
     * 穿戴装备
     */
    @PostMapping("/equip")
    @SaCheckLogin
    @ApiOperation("穿戴装备")
    public BaseResponse<Boolean> equipItem(@RequestBody @Validated ItemEquipRequest equipRequest) {
        if (equipRequest == null || equipRequest.getItemInstanceId() == null || equipRequest.getItemInstanceId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品实例ID不能为空");
        }
        User loginUser = userService.getLoginUser();
        boolean result = itemInstancesService.equipItem(equipRequest.getItemInstanceId(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 卸下装备
     */
    @PostMapping("/unequip")
    @SaCheckLogin
    @ApiOperation("卸下装备")
    public BaseResponse<Boolean> unequipItem(@RequestBody @Validated ItemUnequipRequest unequipRequest) {
        if (unequipRequest == null || unequipRequest.getEquipSlot() == null || unequipRequest.getEquipSlot().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "装备槽位不能为空");
        }
        User loginUser = userService.getLoginUser();
        boolean result = itemInstancesService.unequipItem(unequipRequest.getEquipSlot(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员编辑物品实例信息（可查看模板信息）
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("编辑物品实例信息（管理员）")
    public BaseResponse<ItemInstanceVO> editItemInstance(@RequestBody @Validated ItemInstanceEditRequest itemInstanceEditRequest) {
        if (itemInstanceEditRequest == null || itemInstanceEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ItemInstanceVO vo = itemInstancesService.editItemInstance(itemInstanceEditRequest);
        return ResultUtils.success(vo);
    }

    /**
     * 根据 id 获取物品实例（封装类）
     */
    @GetMapping("/get")
    @ApiOperation("根据 id 获取物品实例）")
    public BaseResponse<ItemInstances> getItemInstanceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        ItemInstances itemInstances = itemInstancesService.getById(id);
        ThrowUtils.throwIf(itemInstances == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(itemInstances);
    }

    // end region

    // region 分页查询

    /**
     * 分页获取物品列表
     *
     * @param itemInstanceQueryRequest 分页查询请求
     * @return 物品列表分页
     */
    @PostMapping("/list/page")
    @ApiOperation(value = "分页获取物品列表")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @SaCheckLogin
    public BaseResponse<Page<ItemInstances>> listItemInstancesByPage(@RequestBody ItemInstanceQueryRequest itemInstanceQueryRequest) {
        if (itemInstanceQueryRequest == null || itemInstanceQueryRequest.getCurrent() <= 0) {
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
        return ResultUtils.success(itemInstancesPage);

    }

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
        if (itemInstanceQueryRequest == null || itemInstanceQueryRequest.getCurrent() <= 0) {
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
        // 按装备模板ID升序排序
        queryWrapper.orderByAsc("templateId");
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
        if (itemInstanceQueryRequest == null || itemInstanceQueryRequest.getCurrent() <= 0) {
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
