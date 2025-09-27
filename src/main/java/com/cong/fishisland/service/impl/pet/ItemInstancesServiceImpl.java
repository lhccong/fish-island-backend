package com.cong.fishisland.service.impl.pet;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.pet.ItemInstancesMapper;
import com.cong.fishisland.model.dto.pet.ItemInstanceQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Shing
 * @description 针对表【item_instances(物品实例表（玩家真正持有的物品，每个实例可有强化、耐久、附魔等个性化信息）)】的数据库操作Service实现
 * @createDate 2025-09-26 15:58:09
 */
@Service
public class ItemInstancesServiceImpl extends ServiceImpl<ItemInstancesMapper, ItemInstances> implements ItemInstancesService {

    @Resource
    ItemTemplatesService itemTemplatesService;

    /**
     * 根据查询条件构造查询参数
     *
     * @param itemInstanceQueryRequest 查询条件
     * @return QueryWrapper<ItemInstances>
     */
    @Override
    public QueryWrapper<ItemInstances> getQueryWrapper(ItemInstanceQueryRequest itemInstanceQueryRequest) {
        QueryWrapper<ItemInstances> queryWrapper = new QueryWrapper<>();
        if (itemInstanceQueryRequest == null) {
            return queryWrapper;
        }
        String category = itemInstanceQueryRequest.getCategory();
        String equipSlot = itemInstanceQueryRequest.getEquipSlot();
        Integer rarity = itemInstanceQueryRequest.getRarity();
        String sortField = itemInstanceQueryRequest.getSortField();
        String sortOrder = itemInstanceQueryRequest.getSortOrder();

        // 基础条件
        queryWrapper.eq("isDelete", 0);

        // 过滤条件
        if (StringUtils.isNotBlank(category)) {
            queryWrapper.eq("category", category);
        }
        if (StringUtils.isNotBlank(equipSlot)) {
            queryWrapper.eq("equipSlot", equipSlot);
        }
        if (rarity != null) {
            queryWrapper.eq("rarity", rarity);
        }

        // 排序
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (SqlUtils.validSortField(sortField)) {
            queryWrapper.orderBy(true, asc, sortField);
        } else {
            queryWrapper.orderByDesc("updateTime");
        }

        return queryWrapper;
    }

    /**
     * 获取物品实例封装
     *
     * @param itemInstancesPage 物品实例页面
     * @return {@link Page}<{@link ItemInstanceVO}>
     */
    @Override
    public Page<ItemInstanceVO> getItemInstancesVoPage(Page<ItemInstances> itemInstancesPage) {
        Page<ItemInstanceVO> voPage = new Page<>(itemInstancesPage.getCurrent(), itemInstancesPage.getSize(), itemInstancesPage.getTotal());
        List<ItemInstances> records = itemInstancesPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return voPage;
        }

        // 1. 批量获取模板ID
        List<Long> templateIds = records.stream()
                .map(ItemInstances::getTemplateId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询模板并转 Map
        Map<Long, ItemTemplateVO> templateVOMap = itemTemplatesService.getTemplateVOMapByIds(templateIds);

        // 3. 构建 VO 列表
        List<ItemInstanceVO> voList = records.stream().map(item -> {
            ItemInstanceVO vo = new ItemInstanceVO();
            BeanUtils.copyProperties(item, vo);
            vo.setTemplate(templateVOMap.get(item.getTemplateId()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}