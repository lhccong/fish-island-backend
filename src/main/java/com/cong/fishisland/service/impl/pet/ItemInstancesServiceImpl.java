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
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    @Resource
    UserPointsService userPointsService;

    @Override
    public Long addItemInstance(ItemInstanceAddRequest itemInstanceAddRequest) {
        // 1. 参数校验
        if (itemInstanceAddRequest == null || itemInstanceAddRequest.getTemplateId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板ID不能为空");
        }

        // 2. 获取物品模板并校验
        ItemTemplates template = itemTemplatesService.getById(itemInstanceAddRequest.getTemplateId());
        if (template == null || template.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板不存在或已删除");
        }

        // 3. 确定持有者ID
        User loginUser = userService.getLoginUser();
        Long loginUserId = loginUser == null ? null : loginUser.getId();
        Long ownerUserId = (itemInstanceAddRequest.getOwnerUserId() != null && itemInstanceAddRequest.getOwnerUserId() > 0)
                ? itemInstanceAddRequest.getOwnerUserId() : loginUserId;
        if (ownerUserId == null || ownerUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标持有者ID非法");
        }

        // 4. 确定添加数量（默认1）
        int addQuantity = (itemInstanceAddRequest.getQuantity() == null || itemInstanceAddRequest.getQuantity() <= 0)
                ? 1 : itemInstanceAddRequest.getQuantity();

        // 5. 查询是否已有相同模板的实例
        ItemInstances existingInstance = this.getOne(new QueryWrapper<ItemInstances>()
                .eq("ownerUserId", ownerUserId)
                .eq("templateId", template.getId())
                .last("LIMIT 1"));

        // 6. 可叠加物品逻辑
        if (template.getStackable() != null && template.getStackable() == 1) {
            if (existingInstance != null) {
                // 已有实例：累加数量
                int newQuantity = (existingInstance.getQuantity() == null ? 0 : existingInstance.getQuantity()) + addQuantity;
                existingInstance.setQuantity(newQuantity);
                this.updateById(existingInstance);
                return existingInstance.getId();
            } else {
                // 创建新实例
                ItemInstances newItem = new ItemInstances();
                BeanUtils.copyProperties(itemInstanceAddRequest, newItem);
                newItem.setTemplateId(template.getId());
                newItem.setOwnerUserId(ownerUserId);
                newItem.setQuantity(addQuantity);
                this.save(newItem);
                return newItem.getId();
            }
        }

        // 7. 不可叠加物品逻辑
        if (existingInstance != null) {
            // 已有实例：新增的物品全部分解为积分
            Long totalPoints = decomposeItemInstance(null, template.getId(), ownerUserId, addQuantity);

            // 抛出业务异常，提示物品已自动分解
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("您已拥有该物品，新增的 %d 件物品已自动分解，获得 %d 积分", addQuantity, totalPoints));
        } else {
            // 创建新实例（数量固定为1）
            ItemInstances newItem = new ItemInstances();
            BeanUtils.copyProperties(itemInstanceAddRequest, newItem);
            newItem.setTemplateId(template.getId());
            newItem.setOwnerUserId(ownerUserId);
            newItem.setQuantity(1);
            this.save(newItem);

            // 如果添加数量 > 1，多余的分解为积分
            if (addQuantity > 1) {
                Long decomposedPoints = decomposeItemInstance(null, template.getId(), ownerUserId, addQuantity - 1);

                // 抛出业务异常，提示部分物品已分解
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        String.format("添加成功！已保留 1 件物品，多余的 %d 件已自动分解，获得 %d 积分",
                                addQuantity - 1, decomposedPoints));
            }

            return newItem.getId();
        }
    }

    /**
     * 更新物品实例信息
     */
    @Override
    public boolean updateItemInstance(ItemInstanceUpdateRequest itemInstanceUpdateRequest) {
        if (itemInstanceUpdateRequest == null || itemInstanceUpdateRequest.getTemplateId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板ID不能为空");
        }

        QueryWrapper<ItemInstances> wrapper = new QueryWrapper<>();
        wrapper.eq("ownerUserId", itemInstanceUpdateRequest.getOwnerUserId())
                .eq("templateId", itemInstanceUpdateRequest.getTemplateId());

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
    public ItemInstanceVO editItemInstance(ItemInstanceEditRequest itemInstanceEditRequest) {
        if (itemInstanceEditRequest == null || itemInstanceEditRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 1. 根据ID查询实例是否存在
        ItemInstances itemInstances = this.getById(itemInstanceEditRequest.getId());
        if (itemInstances == null) {
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

    /**
     * 分解物品实例，将物品转换为用户积分
     * 注意：此方法使用独立事务，即使外层事务回滚，分解操作也会提交
     * 两种使用方式：
     * 1. 用户主动分解背包中的物品：传入 itemInstanceId，其他参数传 null
     * 2. 系统自动分解（如添加重复物品）：传入 templateId、userId、quantity，itemInstanceId 传 null
     *
     * @param itemInstanceId 物品实例ID（场景1必传，场景2传null）
     * @param templateId     物品模板ID（场景2必传，场景1传null）
     * @param userId         用户ID（场景2必传，场景1传null）
     * @param quantity       分解数量（场景2必传，场景1传null）
     * @return 分解获得的积分数量
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long decomposeItemInstance(Long itemInstanceId, Long templateId, Long userId, Integer quantity) {
        // ==================== 场景1：根据物品实例ID分解（用户主动分解） ====================
        if (itemInstanceId != null && itemInstanceId > 0) {
            // 1. 登录用户校验
            User loginUser = userService.getLoginUser();
            Long loginUserId = loginUser == null ? null : loginUser.getId();
            if (loginUserId == null || loginUserId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未登录或ID非法");
            }

            // 2. 查询物品实例
            ItemInstances itemInstance = this.getById(itemInstanceId);
            if (itemInstance == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "物品实例不存在或已删除");
            }

            // 3. 权限校验：仅本人可分解
            if (!itemInstance.getOwnerUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能分解自己的物品");
            }

            // 4. 获取模板信息和分解数量
            templateId = itemInstance.getTemplateId();
            userId = loginUserId;
            quantity = itemInstance.getQuantity();

            // 5. 继续执行下面的统一分解逻辑，最后删除实例
        }

        // ==================== 场景2：根据模板ID和数量直接分解（系统自动分解） ====================
        // 统一的分解逻辑
        if (templateId == null || templateId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "物品模板ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分解数量必须大于0");
        }

        // 1. 查询物品模板
        ItemTemplates template = itemTemplatesService.getById(templateId);
        if (template == null || template.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "物品模板不存在或已删除");
        }

        // 2. 校验是否可分解
        Long removePoint = template.getRemovePoint() == null ? 0L : template.getRemovePoint().longValue();
        if (removePoint <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该物品无法分解（分解积分为0）");
        }

        // 3. 计算分解获得的积分
        Long totalPoints = removePoint * quantity;

        // 4. 发放积分
        userPointsService.updatePoints(userId, totalPoints.intValue(), false);

        // 5. 场景1需要删除物品实例
        if (itemInstanceId != null && itemInstanceId > 0) {
            this.removeById(itemInstanceId);
        }

        return totalPoints;
    }
}