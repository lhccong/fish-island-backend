package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.pet.ItemInstanceQueryRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;

/**
 * @author Shing
 * @description 针对表【item_instances(物品实例表（玩家真正持有的物品，每个实例可有强化、耐久、附魔等个性化信息）)】的数据库操作Service
 * @createDate 2025-09-26 15:58:09
 */
public interface ItemInstancesService extends IService<ItemInstances> {

    /**
     * 获取查询条件
     *
     * @param itemInstanceQueryRequest 查询请求
     * @return {@link QueryWrapper}<{@link ItemInstances}>
     */
    QueryWrapper<ItemInstances> getQueryWrapper(ItemInstanceQueryRequest itemInstanceQueryRequest);

    /**
     * 分页获取物品实例封装
     *
     * @param itemInstancesPage 物品实例分页
     * @return {@link Page}<{@link ItemInstanceVO}>
     */
    Page<ItemInstanceVO> getItemInstancesVoPage(Page<ItemInstances> itemInstancesPage);

}