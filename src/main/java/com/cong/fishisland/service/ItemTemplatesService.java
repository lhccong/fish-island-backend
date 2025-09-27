package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.baomidou.mybatisplus.extension.service.IService;
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
     * 根据模板ID列表批量查询模板信息
     * @param templateIds 模板ID列表
     * @return Map<模板ID, ItemTemplateVO>，便于快速查找
     */
    Map<Long, ItemTemplateVO> getTemplateVOMapByIds(List<Long> templateIds);

}












