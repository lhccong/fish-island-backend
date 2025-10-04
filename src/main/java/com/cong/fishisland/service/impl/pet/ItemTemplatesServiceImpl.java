package com.cong.fishisland.service.impl.pet;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.ItemTemplatesMapper;
import com.cong.fishisland.model.dto.item.ItemTemplateAddRequest;
import com.cong.fishisland.model.dto.item.ItemTemplateEditRequest;
import com.cong.fishisland.model.dto.item.ItemTemplateQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.service.UserPointsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Shing
 * @description 针对表【item_templates(物品模板表（通用配置，包括装备、消耗品、材料等）)】的数据库操作Service实现
 * @createDate 2025-09-26 15:58:08
 */
@Service
public class ItemTemplatesServiceImpl extends ServiceImpl<ItemTemplatesMapper, ItemTemplates> implements ItemTemplatesService {

    @Resource
    private ItemInstancesService itemInstancesService;

    @Resource
    private UserPointsService userPointsService;

    @Override
    public Long addItemTemplate(ItemTemplateAddRequest itemTemplateAddRequest) {
        if (itemTemplateAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        // 基础字段校验
        if (StringUtils.isBlank(itemTemplateAddRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板code不能为空");
        }
        if (StringUtils.isBlank(itemTemplateAddRequest.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品名称不能为空");
        }
        if (StringUtils.isBlank(itemTemplateAddRequest.getCategory())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品大类不能为空");
        }
        // 唯一性校验：code 不可重复
        long existed = this.lambdaQuery()
                .eq(ItemTemplates::getCode, itemTemplateAddRequest.getCode())
                .eq(ItemTemplates::getIsDelete, 0)
                .count();
        if (existed > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "模板code已存在");
        }
        ItemTemplates itemTemplates = new ItemTemplates();
        BeanUtils.copyProperties(itemTemplateAddRequest, itemTemplates);
        // 默认值兜底
        if (itemTemplates.getStackable() == null) {
            itemTemplates.setStackable(0);
        }
        if (itemTemplates.getRarity() == null) {
            itemTemplates.setRarity(1);
        }
        itemTemplates.setIsDelete(0);
        // 保存到数据库
        boolean saveResult = this.save(itemTemplates);
        if (!saveResult || itemTemplates.getId() == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增物品模板失败");
        }
        // 3. 返回生成的主键 ID
        return itemTemplates.getId();
    }

    @Override
    public ItemTemplateVO getItemTemplateVOById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板ID不合法");
        }
        
        ItemTemplates itemTemplates = this.getById(id);
        if (itemTemplates == null || itemTemplates.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "物品模板不存在");
        }
        
        ItemTemplateVO itemTemplateVO = new ItemTemplateVO();
        BeanUtils.copyProperties(itemTemplates, itemTemplateVO);
        return itemTemplateVO;
    }

    @Override
    public ItemTemplateVO editItemTemplate(ItemTemplateEditRequest itemTemplateEditRequest) {
        if (itemTemplateEditRequest == null || itemTemplateEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        
        // 通过ID校验：检查传入的ID在数据库中是否存在，存在才能修改
        ItemTemplates itemTemplates = this.getById(itemTemplateEditRequest.getId());
        if (itemTemplates == null || itemTemplates.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "物品模板不存在");
        }
        
        // 如果提供了code，需要校验code的唯一性（排除自身）
        String requestCode = itemTemplateEditRequest.getCode();
        if (StringUtils.isNotBlank(requestCode)) {
            // 去除首尾空白
            requestCode = requestCode.trim();
            itemTemplateEditRequest.setCode(requestCode);
            
            // 唯一性校验：排除自身
            long existed = this.lambdaQuery()
                    .eq(ItemTemplates::getCode, requestCode)
                    .eq(ItemTemplates::getIsDelete, 0)
                    .ne(ItemTemplates::getId, itemTemplateEditRequest.getId())
                    .count();
            if (existed > 0) {
            	throw new BusinessException(ErrorCode.OPERATION_ERROR, "模板code已存在");
            }
        }

        // 复制前端传来的数据到数据库对象
        BeanUtils.copyProperties(itemTemplateEditRequest, itemTemplates);
        
        // 执行数据库更新操作
        boolean result = this.updateById(itemTemplates);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新物品模板失败");
        }

        ItemTemplateVO itemTemplateVO = new ItemTemplateVO();
        BeanUtils.copyProperties(itemTemplates, itemTemplateVO);
        return itemTemplateVO;
    }

    @Override
    public QueryWrapper<ItemTemplates> getQueryWrapper(ItemTemplateQueryRequest itemTemplateQueryRequest) {
        QueryWrapper<ItemTemplates> queryWrapper = new QueryWrapper<>();
        if (itemTemplateQueryRequest == null) {
            return queryWrapper;
        }

        Long id = itemTemplateQueryRequest.getId();
        String code = itemTemplateQueryRequest.getCode();
        String name = itemTemplateQueryRequest.getName();
        String category = itemTemplateQueryRequest.getCategory();
        String subType = itemTemplateQueryRequest.getSubType();
        Integer stackable = itemTemplateQueryRequest.getStackable();

        // 精确匹配
        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        if (StringUtils.isNotBlank(code)) {
            queryWrapper.eq("code", code);
        }
        if (StringUtils.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }
        if (StringUtils.isNotBlank(subType)) {
            queryWrapper.eq("sub_type", subType);
        }
        if (stackable != null) {
            queryWrapper.eq("stackable", stackable);
        }

        // 模糊查询
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }

        // 逻辑未删除
        queryWrapper.eq("isDelete", 0);

        // 按更新时间倒序
        queryWrapper.orderByDesc("updateTime");

        return queryWrapper;
    }

    @Override
    public Page<ItemTemplateVO> getItemTemplateVOVoPage(Page<ItemTemplates> itemTemplatesPage) {
        Page<ItemTemplateVO> voPage = new Page<>(itemTemplatesPage.getCurrent(),
                itemTemplatesPage.getSize(),
                itemTemplatesPage.getTotal());

        List<ItemTemplateVO> voList = itemTemplatesPage.getRecords().stream().map(item -> {
            ItemTemplateVO vo = new ItemTemplateVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Map<Long, ItemTemplateVO> getTemplateVOMapByIds(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 批量查询模板
        List<ItemTemplates> itemTemplates = this.listByIds(templateIds);

        // 转 VO 并转换成 Map<id, VO>
        return itemTemplates.stream().collect(Collectors.toMap(
                ItemTemplates::getId,
                template -> {
                    ItemTemplateVO itemTemplateVO = new ItemTemplateVO();
                    BeanUtils.copyProperties(template, itemTemplateVO);
                    return itemTemplateVO;
                }
        ));
    }
}