package com.cong.fishisland.service.impl.pet;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.pet.ItemInstancesMapper;
import com.cong.fishisland.model.dto.item.ItemInstanceAddRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceEditRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceQueryRequest;
import com.cong.fishisland.model.dto.item.ItemInstanceUpdateRequest;
import com.cong.fishisland.model.entity.pet.ItemInstances;
import com.cong.fishisland.model.entity.pet.ItemTemplates;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.pet.ItemInstanceVO;
import com.cong.fishisland.model.vo.pet.ItemTemplateVO;
import com.cong.fishisland.service.ItemInstancesService;
import com.cong.fishisland.service.ItemTemplatesService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addItemInstance(ItemInstanceAddRequest itemInstanceAddRequest) {
        if (itemInstanceAddRequest == null || itemInstanceAddRequest.getTemplateId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板ID不能为空");
        }
        // 1. 获取物品模板
        ItemTemplates template = itemTemplatesService.getById(itemInstanceAddRequest.getTemplateId());
        if (template == null || template.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板不存在或已删除");
        }

        // 2. 获取持有者ID
        User loginUser = userService.getLoginUser();
        Long ownerUserId = (itemInstanceAddRequest.getOwnerUserId() != null && !itemInstanceAddRequest.getOwnerUserId().equals(0L))
                ? itemInstanceAddRequest.getOwnerUserId()
                : loginUser.getId();

        // 3. 可叠加处理
        if (template.getStackable() == 1) {
            // 查询是否已有相同模板的物品
            ItemInstances existingItem = this.getOne(new QueryWrapper<ItemInstances>()
                    .eq("ownerUserId", ownerUserId)
                    .eq("templateId", template.getId()));

            if (existingItem != null) {
                // 已存在则数量增加
                int addQuantity = itemInstanceAddRequest.getQuantity() != null && itemInstanceAddRequest.getQuantity() > 0
                        ? itemInstanceAddRequest.getQuantity() : 1;
                existingItem.setQuantity(existingItem.getQuantity() + addQuantity);
                this.updateById(existingItem);
                return existingItem.getId();
            }
        }

        // 4. 不可叠加或不存在的物品，创建新实例
        ItemInstances newItem = new ItemInstances();
        newItem.setTemplateId(template.getId());
        newItem.setOwnerUserId(ownerUserId);
        // 设置默认数量
        newItem.setQuantity(itemInstanceAddRequest.getQuantity() != null && itemInstanceAddRequest.getQuantity() > 0
                ? itemInstanceAddRequest.getQuantity() : 1);

        this.save(newItem);

        return newItem.getId();
    }

    /**
     * 更新物品实例信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateItemInstance(ItemInstanceUpdateRequest itemInstanceUpdateRequest) {
        if (itemInstanceUpdateRequest == null || itemInstanceUpdateRequest.getTemplateId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板ID不能为空");
        }

        QueryWrapper<ItemInstances> wrapper = new QueryWrapper<>();
        wrapper.eq("ownerUserId", itemInstanceUpdateRequest.getOwnerUserId())
                .eq("templateId", itemInstanceUpdateRequest.getTemplateId())
                .eq("isDelete", 0);

        ItemInstances item = this.getOne(wrapper, false);
        if (item == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品实例不存在");
        }

        if (itemInstanceUpdateRequest.getQuantity() != null && itemInstanceUpdateRequest.getQuantity() > 0) {
            item.setQuantity(itemInstanceUpdateRequest.getQuantity());
        }
        return this.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemInstanceVO editItemInstance(ItemInstanceEditRequest itemInstanceEditRequest) {
        if (itemInstanceEditRequest == null || itemInstanceEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 1. 根据ID查询实例是否存在
        ItemInstances itemInstances = this.getById(itemInstanceEditRequest.getId());
        if (itemInstances == null || itemInstances.getIsDelete() != null && itemInstances.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "物品实例不存在");
        }

        // 2. 权限校验：仅本人或管理员可编辑
        User loginUser = userService.getLoginUser();
        if (!itemInstances.getOwnerUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限编辑该物品实例");
        }

        // 3. 直接复制请求数据到实体对象
        BeanUtils.copyProperties(itemInstanceEditRequest, itemInstances);

        // 4. 特殊处理：数量必须大于0
        if (itemInstances.getQuantity() != null && itemInstances.getQuantity() <= 0) {
            itemInstances.setQuantity(null);
        }

        // 5. 校验模板存在性（如果模板ID被更新）
        if (itemInstanceEditRequest.getTemplateId() != null) {
            ItemTemplates template = itemTemplatesService.getById(itemInstanceEditRequest.getTemplateId());
            if (template == null || template.getIsDelete() == 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "关联的物品模板不存在");
            }
            itemInstances.setTemplateId(itemInstanceEditRequest.getTemplateId());
        }


        boolean updated = this.updateById(itemInstances);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新物品实例失败");
        }

        // 直接返回更新后的VO
        ItemInstanceVO vo = new ItemInstanceVO();
        BeanUtils.copyProperties(itemInstances, vo);
        return vo;
    }

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
        // 排序字段处理
        String sortField = itemInstanceQueryRequest.getSortField();
        String sortOrder = itemInstanceQueryRequest.getSortOrder();
        boolean asc = "asc".equalsIgnoreCase(sortOrder);

        if (StringUtils.isNotBlank(sortField) && SqlUtils.validSortField(sortField)) {
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