package com.cong.fishisland.service.impl.user;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.VipTypeConstant;
import com.cong.fishisland.mapper.user.UserVipMapper;
import com.cong.fishisland.model.dto.user.UserVipAddRequest;
import com.cong.fishisland.model.dto.user.UserVipQueryRequest;
import com.cong.fishisland.model.dto.user.UserVipUpdateRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.model.enums.VipTypeEnum;
import com.cong.fishisland.model.vo.user.UserVipVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserVipService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户会员表(UserVip)表服务实现类
 *
 * @author cong
 * @description 针对表【user_vip(用户会员表)】的数据库操作Service实现
 * @createDate 2025-05-01 10:00:00
 */
@Slf4j
@Service
public class UserVipServiceImpl extends ServiceImpl<UserVipMapper, UserVip>
        implements UserVipService {

    @Resource
    private UserService userService;

    @Override
    public QueryWrapper<UserVip> getQueryWrapper(UserVipQueryRequest userVipQueryRequest) {
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        if (userVipQueryRequest == null) {
            return queryWrapper;
        }
        
        // 从对象中取值
        Long id = userVipQueryRequest.getId();
        Long userId = userVipQueryRequest.getUserId();
        String cardNo = userVipQueryRequest.getCardNo();
        Integer type = userVipQueryRequest.getType();
        String createTimeStart = userVipQueryRequest.getCreateTimeStart();
        String createTimeEnd = userVipQueryRequest.getCreateTimeEnd();
        String updateTimeStart = userVipQueryRequest.getUpdateTimeStart();
        String updateTimeEnd = userVipQueryRequest.getUpdateTimeEnd();
        String sortField = userVipQueryRequest.getSortField();
        String sortOrder = userVipQueryRequest.getSortOrder();

        // 补充需要的查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(cardNo), "cardNo", cardNo);
        queryWrapper.eq(ObjectUtils.isNotEmpty(type), "type", type);
        
        // 创建时间范围
        queryWrapper.ge(StringUtils.isNotBlank(createTimeStart), "createTime", createTimeStart);
        queryWrapper.le(StringUtils.isNotBlank(createTimeEnd), "createTime", createTimeEnd);
        
        // 更新时间范围
        queryWrapper.ge(StringUtils.isNotBlank(updateTimeStart), "updateTime", updateTimeStart);
        queryWrapper.le(StringUtils.isNotBlank(updateTimeEnd), "updateTime", updateTimeEnd);
        
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_DESC),
                sortField);
        return queryWrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVip(UserVipAddRequest userVipAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(userVipAddRequest == null, ErrorCode.PARAMS_ERROR, "创建会员失败，参数为空");
        
        Long userId = userVipAddRequest.getUserId();
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "创建会员失败，用户ID为空");
        
        // 检查用户是否存在
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "创建会员失败，用户不存在");
        
        // 检查会员类型
        Integer type = userVipAddRequest.getType();
        ThrowUtils.throwIf(type == null || !VipTypeEnum.getValues().contains(type), 
                ErrorCode.PARAMS_ERROR, "创建会员失败，会员类型错误");
        
        // 检查月卡会员是否设置了有效期
        if (VipTypeConstant.MONTHLY.equals(type)) {
            ThrowUtils.throwIf(userVipAddRequest.getValidDays() == null, ErrorCode.PARAMS_ERROR, "创建会员失败，月卡会员必须设置有效期");
        }
        
        // 查询用户是否已经是会员
        UserVip existingVip = baseMapper.selectByUserIdForUpdate(userId);
        
        if (existingVip != null) {
            // 如果用户已经是永久会员，则不能再次创建会员
            if (VipTypeConstant.PERMANENT.equals(existingVip.getType())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "该用户已经是永久会员");
            }
            
            // 如果用户是月卡会员，则更新有效期
            if (VipTypeConstant.MONTHLY.equals(type)) {
                // 如果原有会员未过期，则延长有效期
                Date now = new Date();
                Date validDays = userVipAddRequest.getValidDays();
                if (existingVip.getValidDays() != null && existingVip.getValidDays().after(now)) {
                    // 计算剩余有效期
                    long remainingTime = existingVip.getValidDays().getTime() - now.getTime();
                    // 新的有效期 = 新设置的有效期 + 剩余有效期
                    validDays = new Date(validDays.getTime() + remainingTime);
                }
                existingVip.setValidDays(validDays);
            } else {
                // 如果用户是月卡会员，升级为永久会员
                existingVip.setType(VipTypeConstant.PERMANENT);
                existingVip.setValidDays(null); // 永久会员无有效期
            }
            
            // 更新卡号
            if (StringUtils.isNotBlank(userVipAddRequest.getCardNo())) {
                existingVip.setCardNo(userVipAddRequest.getCardNo());
            }
            
            boolean update = this.updateById(existingVip);
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新会员信息失败");
            return existingVip.getId();
        }
        
        // 创建新会员
        UserVip userVip = new UserVip();
        BeanUtils.copyProperties(userVipAddRequest, userVip);
        
        // 永久会员无有效期
        if (VipTypeConstant.PERMANENT.equals(type)) {
            userVip.setValidDays(null);
        }
        
        boolean save = this.save(userVip);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "创建会员失败");
        return userVip.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVip(UserVipUpdateRequest userVipUpdateRequest) {
        // 参数校验
        ThrowUtils.throwIf(userVipUpdateRequest == null, ErrorCode.PARAMS_ERROR, "更新会员失败，参数为空");
        
        Long id = userVipUpdateRequest.getId();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "更新会员失败，会员ID为空");
        
        // 检查会员是否存在
        UserVip userVip = this.getById(id);
        ThrowUtils.throwIf(userVip == null, ErrorCode.NOT_FOUND_ERROR, "更新会员失败，会员不存在");
        
        // 检查会员类型
        Integer type = userVipUpdateRequest.getType();
        if (type != null) {
            ThrowUtils.throwIf(!VipTypeEnum.getValues().contains(type), ErrorCode.PARAMS_ERROR, "更新会员失败，会员类型错误");
            
            // 如果是永久会员，则清空有效期
            if (VipTypeConstant.PERMANENT.equals(type)) {
                userVipUpdateRequest.setValidDays(null);
            } else if (VipTypeConstant.MONTHLY.equals(type) && userVipUpdateRequest.getValidDays() == null && 
                    VipTypeConstant.PERMANENT.equals(userVip.getType())) {
                // 如果从永久会员降级为月卡会员，必须设置有效期
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新会员失败，月卡会员必须设置有效期");
            }
        }
        
        // 更新会员信息
        UserVip updateVip = new UserVip();
        BeanUtils.copyProperties(userVipUpdateRequest, updateVip);
        boolean update = this.updateById(updateVip);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新会员信息失败");
        return true;
    }

    @Override
    public UserVipVO getVipVO(UserVip userVip) {
        if (userVip == null) {
            return null;
        }
        
        UserVipVO userVipVO = UserVipVO.objToVo(userVip);
        
        // 关联查询用户信息
        Long userId = userVip.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            userVipVO.setUser(userService.getUserVO(user));
        }
        
        return userVipVO;
    }

    @Override
    public Page<UserVipVO> getVipVOPage(Page<UserVip> userVipPage) {
        List<UserVip> userVipList = userVipPage.getRecords();
        Page<UserVipVO> userVipVOPage = new Page<>(userVipPage.getCurrent(), userVipPage.getSize(), userVipPage.getTotal());
        
        if (CollUtil.isEmpty(userVipList)) {
            return userVipVOPage;
        }
        
        // 关联查询用户信息
        List<Long> userIds = userVipList.stream().map(UserVip::getUserId).collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        
        // 构建 userId -> User 的映射
        java.util.Map<Long, User> userIdUserMap = users.stream().collect(Collectors.toMap(User::getId, user -> user));
        
        // 填充会员VO对象
        List<UserVipVO> userVipVOList = userVipList.stream().map(userVip -> {
            UserVipVO userVipVO = UserVipVO.objToVo(userVip);
            Long userId = userVip.getUserId();
            User user = userIdUserMap.get(userId);
            userVipVO.setUser(userService.getUserVO(user));
            return userVipVO;
        }).collect(Collectors.toList());
        
        userVipVOPage.setRecords(userVipVOList);
        return userVipVOPage;
    }

    @Override
    public boolean isUserVip(Long userId) {
        if (userId == null) {
            return false;
        }
        
        // 查询用户会员信息
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        UserVip userVip = this.getOne(queryWrapper);
        
        if (userVip == null) {
            return false;
        }
        
        // 如果是永久会员，直接返回true
        if (VipTypeConstant.PERMANENT.equals(userVip.getType())) {
            return true;
        }
        
        // 如果是月卡会员，检查是否过期
        Date now = new Date();
        return userVip.getValidDays() != null && now.before(userVip.getValidDays());
    }

    @Override
    public boolean isPermanentVip(Long userId) {
        if (userId == null) {
            return false;
        }
        
        // 查询用户会员信息
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("type", VipTypeConstant.PERMANENT); // 永久会员
        queryWrapper.eq("isDelete", 0);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean isVipExpired(Long userId) {
        if (userId == null) {
            return true;
        }
        
        // 查询用户会员信息
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        UserVip userVip = this.getOne(queryWrapper);
        
        if (userVip == null) {
            return true;
        }
        
        // 永久会员不会过期
        if (VipTypeConstant.PERMANENT.equals(userVip.getType())) {
            return false;
        }
        
        // 月卡会员，检查是否过期
        Date now = new Date();
        return userVip.getValidDays() == null || now.after(userVip.getValidDays());
    }
} 