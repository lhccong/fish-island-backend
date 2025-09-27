package com.cong.fishisland.service.impl.pet;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.mapper.pet.ItemTemplatesMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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