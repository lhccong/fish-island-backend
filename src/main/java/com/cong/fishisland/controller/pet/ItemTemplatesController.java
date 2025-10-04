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
import com.cong.fishisland.model.dto.item.ItemTemplateAddRequest;
import com.cong.fishisland.model.dto.item.ItemTemplateEditRequest;
import com.cong.fishisland.model.dto.item.ItemTemplateQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;
import com.cong.fishisland.service.ItemTemplatesService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 物品模板接口
 *
 * @author Shing
 * date 26/9/2025 星期五
 */
@RestController
@RequestMapping("/itemTemplates")
public class ItemTemplatesController {

    @Resource
    private ItemTemplatesService itemTemplatesService;

    // region 增删改查

    /**
     * 添加物品
     */
    @PostMapping("/add")
    @SaCheckLogin
    @ApiOperation("添加物品")
    public BaseResponse<Long> addItemTemplate(@RequestBody ItemTemplateAddRequest itemTemplateAddRequest) {
        if (itemTemplateAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        Long itemTemplateId = itemTemplatesService.addItemTemplate(itemTemplateAddRequest);
        return ResultUtils.success(itemTemplateId);
    }

    /**
     * 删除物品模板
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("删除物品模板")
    public BaseResponse<Boolean> deleteItemTemplate(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id;
        try {
            id = Long.parseLong(deleteRequest.getId());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "id 非法");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        ItemTemplates oldItemTemplate = itemTemplatesService.getById(id);
        ThrowUtils.throwIf(oldItemTemplate == null, ErrorCode.NOT_FOUND_ERROR);
        boolean b = itemTemplatesService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 根据ID获取物品模板详情
     */
    @PostMapping("/get/vo")
    @SaCheckLogin
    @ApiOperation("根据ID获取物品模板详情")
    public BaseResponse<ItemTemplateVO> getItemTemplateVOById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ItemTemplateVO itemTemplateVO = itemTemplatesService.getItemTemplateVOById(id);
        return ResultUtils.success(itemTemplateVO);
    }

    /**
     * 管理员编辑物品模板
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation("编辑物品模板信息（管理员）")
    public BaseResponse<ItemTemplateVO> editItemTemplate(@RequestBody ItemTemplateEditRequest itemTemplateEditRequest) {
        if (itemTemplateEditRequest == null || itemTemplateEditRequest.getId() == null || itemTemplateEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ItemTemplateVO itemTemplateVO = itemTemplatesService.editItemTemplate(itemTemplateEditRequest);
        return ResultUtils.success(itemTemplateVO);
    }

    /**
     * 分页获取物品模板列表
     *
     * @param itemTemplateQueryRequest 分页查询请求
     * @return 物品模板分页
     */
    @PostMapping("/list/page/vo")
    @ApiOperation(value = "分页获取物品模板列表")
    @SaCheckLogin
    public BaseResponse<Page<ItemTemplateVO>> listItemTemplatesVOByPage(@RequestBody ItemTemplateQueryRequest itemTemplateQueryRequest) {
        if (itemTemplateQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        long current = itemTemplateQueryRequest.getCurrent();
        long pageSize = itemTemplateQueryRequest.getPageSize();

        // 限制单页最大数量，防止爬虫或恶意请求
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "单页最多查询50条");
        ThrowUtils.throwIf(pageSize <= 0, ErrorCode.PARAMS_ERROR, "分页大小必须大于0");
        if (current <= 0) {
            current = 1;
        }
        // 1. 构造查询条件
        QueryWrapper<ItemTemplates> queryWrapper = itemTemplatesService.getQueryWrapper(itemTemplateQueryRequest);
        // 2. 分页查询物品模板
        Page<ItemTemplates> itemTemplatesPage = itemTemplatesService.page(new Page<>(current, pageSize), queryWrapper);
        // 3. 转换 VO 并返回
        Page<ItemTemplateVO> itemTemplateVOPage = itemTemplatesService.getItemTemplateVOVoPage(itemTemplatesPage);
        return ResultUtils.success(itemTemplateVOPage);
    }

    // endregion

}