package com.cong.fishisland.service.impl.props;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.constant.PropsTypeConstant;
import com.cong.fishisland.mapper.props.PropsMapper;
import com.cong.fishisland.model.dto.props.PropsQueryRequest;
import com.cong.fishisland.model.dto.props.PropsPurchaseRequest;
import com.cong.fishisland.model.entity.props.Props;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserVip;
import com.cong.fishisland.model.vo.props.PropsVO;
import com.cong.fishisland.service.PropsService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserVipService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author cong
 * @description 针对表【props(道具表)】的数据库操作Service实现
 * @createDate 2025-05-10 16:00:00
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PropsServiceImpl extends ServiceImpl<PropsMapper, Props> implements PropsService {

    private final UserService userService;
    private final UserPointsService userPointsService;
    private final UserVipService userVipService;

    @Override
    public QueryWrapper<Props> getQueryWrapper(PropsQueryRequest propsQueryRequest) {
        if (propsQueryRequest == null) {
            return new QueryWrapper<>();
        }
        String name = propsQueryRequest.getName();
        String type = propsQueryRequest.getType();
        String sortField = propsQueryRequest.getSortField();
        String sortOrder = propsQueryRequest.getSortOrder();

        QueryWrapper<Props> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq("type", type);
        }
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public PropsVO getPropsVO(Props props) {
        if (props == null) {
            return null;
        }
        PropsVO propsVO = new PropsVO();
        org.springframework.beans.BeanUtils.copyProperties(props, propsVO);
        return propsVO;
    }

    @Override
    public Page<PropsVO> getPropsVOPage(Page<Props> propsPage) {
        List<Props> propsList = propsPage.getRecords();
        Page<PropsVO> propsVOPage = new Page<>(propsPage.getCurrent(), propsPage.getSize(), propsPage.getTotal());
        if (ObjectUtils.isEmpty(propsList)) {
            return propsVOPage;
        }
        List<PropsVO> propsVOList = propsList.stream().map(this::getPropsVO).collect(Collectors.toList());
        propsVOPage.setRecords(propsVOList);
        return propsVOPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean purchaseProps(PropsPurchaseRequest propsPurchaseRequest) {
        if (propsPurchaseRequest == null || propsPurchaseRequest.getPropsId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 1. 检查道具是否存在
        Long propsId = propsPurchaseRequest.getPropsId();
        Props props = this.getById(propsId);
        ThrowUtils.throwIf(props == null, ErrorCode.NOT_FOUND_ERROR, "道具不存在");
        
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();
        
        // 3. 扣除用户积分
        userPointsService.deductPoints(userId, props.getPoints());
        
        // 4. 根据道具类型进行不同的处理
        String propsType = props.getType();
        switch (propsType) {
            case PropsTypeConstant.VIP_MONTHLY:
                // 处理会员月卡
                handleVipMonthly(userId);
                break;
//            case PropsTypeConstant.TITLE:
//                // 处理称号
//                handleTitle(loginUser, props);
//                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的道具类型");
        }
        
        return true;
    }
    
    /**
     * 处理会员月卡
     * 
     * @param userId 用户ID
     */
    private void handleVipMonthly(Long userId) {
        // 检查用户是否已经是永久会员
        boolean isPermanentVip = userVipService.isPermanentVip(userId);
        if (isPermanentVip) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经是永久会员，无需购买月卡");
        }
        
        // 查询用户当前会员信息
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        UserVip userVip = userVipService.getOne(queryWrapper);
        
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        
        if (userVip == null) {
            // 用户不是会员，创建新的会员记录
            userVip = new UserVip();
            userVip.setUserId(userId);
            // 月卡会员
            userVip.setType(1);
            calendar.setTime(now);
            // 增加一个月
            calendar.add(Calendar.MONTH, 1);
            userVip.setValidDays(calendar.getTime());
            userVipService.save(userVip);
        } else {
            // 用户已经是会员，延长会员时间
            Date validDays = userVip.getValidDays();
            if (validDays == null || validDays.before(now)) {
                // 会员已过期，从当前时间开始计算
                calendar.setTime(now);
            } else {
                // 会员未过期，从过期时间开始计算
                calendar.setTime(validDays);
            }
            // 增加一个月
            calendar.add(Calendar.MONTH, 1);
            userVip.setValidDays(calendar.getTime());
            userVipService.updateById(userVip);
        }
    }
    
    /**
     * 处理称号
     * 
     * @param user 用户
     * @param props 道具
     */
    private void handleTitle(User user, Props props) {
        // 获取用户当前称号列表
        List<String> titleIds = Optional.ofNullable(JSON.parseArray(user.getTitleIdList(), String.class))
                .orElse(new ArrayList<>());
        
        // 检查用户是否已拥有该称号
        if (titleIds.contains(props.getFrameId().toString())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已拥有该称号");
        }
        
        // 添加称号到用户背包
        titleIds.add(props.getFrameId().toString());
        user.setTitleIdList(JSON.toJSONString(titleIds));
        userService.updateById(user);
    }
} 