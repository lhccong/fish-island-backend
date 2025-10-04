package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.item.ItemInstanceAddRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceEditRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceQueryRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceUpdateRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;

/**
 * @author Shing
 * @description 针对表【item_instances(物品实例表（玩家真正持有的物品，每个实例可有强化、耐久、附魔等个性化信息）)】的数据库操作Service
 * @createDate 2025-09-26 15:58:09
 */
public interface ItemInstancesService extends IService<ItemInstances> {


    /**
     * 添加物品实例
     * 如果物品可叠加且已有相同模板，则数量增加
     * 如果不可叠加，则创建新实例或分解成积分
     *
     * @param itemInstanceAddRequest 添加请求
     * @return 新增或更新的物品实例ID
     */
    Long addItemInstance(ItemInstanceAddRequest itemInstanceAddRequest);

    /**
     * 更新物品实例信息（如数量、绑定状态、强化等级、附加属性等）
     *
     * @param itemInstanceUpdateRequest 更新请求
     * @return 是否成功
     */
    boolean updateItemInstance(ItemInstanceUpdateRequest itemInstanceUpdateRequest);

    /**
     * 编辑物品实例信息（返回编辑后的视图对象）
     *
     * @param itemInstanceEditRequest 编辑请求
     * @return 编辑后的视图对象
     */
    ItemInstanceVO editItemInstance(ItemInstanceEditRequest itemInstanceEditRequest);

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