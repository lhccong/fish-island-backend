package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.item.ItemTemplateAddRequest;
import com.cong.fishisland.model.dto.item.ItemTemplateEditRequest;
import com.cong.fishisland.model.dto.item.ItemTemplateQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;

import java.util.List;
import java.util.Map;

/**
 * @author Shing
 * @description 针对表【item_templates(物品模板表（通用配置，包括装备、消耗品、材料等）)】的数据库操作Service
 * @createDate 2025-09-26 15:58:09
 */
public interface ItemTemplatesService extends IService<ItemTemplates> {

    /**
     * 添加物品模板
     */
    Long addItemTemplate(ItemTemplateAddRequest itemTemplateAddRequest);

    /**
     * 根据ID获取物品模板VO，用于编辑页面显示
     *
     * @param id 物品模板ID
     * @return 物品模板VO对象
     */
    ItemTemplateVO getItemTemplateVOById(Long id);

    /**
     * 根据ID获取物品实例VO，用于编辑页面显示
     *
     * @param itemTemplateEditRequest 编辑请求
     * @return 物品实例VO对象
     */
    ItemTemplateVO editItemTemplate(ItemTemplateEditRequest itemTemplateEditRequest);

    /**
     * 获取查询条件
     *
     * @param itemTemplateQueryRequest 查询请求
     * @return {@link QueryWrapper}<{@link ItemTemplates}>
     */
    QueryWrapper<ItemTemplates> getQueryWrapper(ItemTemplateQueryRequest itemTemplateQueryRequest);

    /**
     * 分页获取物品模板封装
     *
     * @param itemTemplatesPage 物品模板分页
     * @return {@link Page}<{@link ItemTemplateVO}>
     */
    Page<ItemTemplateVO> getItemTemplateVOVoPage(Page<ItemTemplates> itemTemplatesPage);

    /**
     * 根据模板ID列表批量查询模板信息
     *
     * @param templateIds 模板ID列表
     * @return Map<模板ID, ItemTemplateVO>，便于快速查找
     */
    Map<Long, ItemTemplateVO> getTemplateVOMapByIds(List<Long> templateIds);

}