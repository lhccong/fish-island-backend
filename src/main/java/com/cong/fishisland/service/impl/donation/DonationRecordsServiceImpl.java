package com.cong.fishisland.service.impl.donation;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.mapper.donation.DonationRecordsMapper;
import com.cong.fishisland.model.dto.donation.DonationRecordsAddRequest;
import com.cong.fishisland.model.dto.donation.DonationRecordsQueryRequest;
import com.cong.fishisland.model.entity.donation.DonationRecords;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.donation.DonationRecordsVO;
import com.cong.fishisland.model.vo.user.LoginUserVO;
import com.cong.fishisland.model.vo.user.UserVO;
import com.cong.fishisland.service.DonationRecordsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户打赏记录表(DonationRecords)表服务实现类
 * @author Shing
 * @description 针对表【donation_records(用户打赏记录表)】的数据库操作Service实现
 * @createDate 2025-04-21 15:07:19
 */
@Slf4j
@Service
public class DonationRecordsServiceImpl extends ServiceImpl<DonationRecordsMapper, DonationRecords>
        implements DonationRecordsService {

    @Resource
    UserService userService;

    @Override
    public QueryWrapper<DonationRecords> getQueryWrapper(DonationRecordsQueryRequest donationRecordsQueryRequest) {
        QueryWrapper<DonationRecords> queryWrapper = new QueryWrapper<>();
        if (donationRecordsQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = donationRecordsQueryRequest.getId();
        Long userId = donationRecordsQueryRequest.getUserId();
        BigDecimal amount = donationRecordsQueryRequest.getAmount();
        String remark = donationRecordsQueryRequest.getRemark();
        String sortField = donationRecordsQueryRequest.getSortField();
        String sortOrder = donationRecordsQueryRequest.getSortOrder();

        // 补充需要的查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(amount), "amount", amount);
        queryWrapper.like(StringUtils.isNotBlank(remark), "remark", remark);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_DESC),
                sortField);
        return queryWrapper;
    }

    // region 增删改查

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(DonationRecordsAddRequest donationRecordsAddRequest) {
        // 参数校验
        if (donationRecordsAddRequest == null || donationRecordsAddRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新增打赏记录失败");
        }

        Long userId = donationRecordsAddRequest.getUserId();
        BigDecimal amount = donationRecordsAddRequest.getAmount();
        String newRemark = donationRecordsAddRequest.getRemark();

        // 1. 加锁查询
        DonationRecords existing = baseMapper.selectByUserIdForUpdate(userId);

        if (existing != null) {
            // userId 存在，累加 amount（必须大于等于 0）
            if (amount != null && amount.compareTo(BigDecimal.ZERO) >= 0) {
                existing.setAmount(existing.getAmount().add(amount));
            }

            // 仅在 remark 有新内容时更新
            if (StringUtils.isNotBlank(newRemark)) {
                existing.setRemark(newRemark);
            }

            boolean update = this.updateById(existing);
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "更新打赏记录失败");
            return existing.getId();
        }

        // userId 不存在，插入新记录
        DonationRecords toInsert = new DonationRecords();
        BeanUtils.copyProperties(donationRecordsAddRequest, toInsert);

        // 确保 amount 合法
        if (toInsert.getAmount() == null || toInsert.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "打赏金额非法");
        }
        boolean insert = this.save(toInsert);
        ThrowUtils.throwIf(!insert, ErrorCode.OPERATION_ERROR, "新增打赏记录失败");

        return toInsert.getId();
    }

    @Override
    public DonationRecordsVO getRecordVO(DonationRecords donationRecords) {
        DonationRecordsVO donationRecordsVO = DonationRecordsVO.objToVo(donationRecords);
        // 1. 关联查询用户信息
        Long userId = donationRecords.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        LoginUserVO loginUserVO = userService.getLoginUserVO(user);
        loginUserVO.setEmail(null);
        donationRecordsVO.setDonorUser(loginUserVO);

        return donationRecordsVO;
    }

    /**
     * 分页查询打赏排行列表（倒序）
     *
     * @param donationRecordsPage 打赏记录分页
     * @return 打赏记录分页
     */
    @Override
    public Page<DonationRecordsVO> getRecordsVOPage(Page<DonationRecords> donationRecordsPage) {
        List<DonationRecords> donationRecordsList = donationRecordsPage.getRecords();
        Page<DonationRecordsVO> donationRecordsVoPage = new Page<>(donationRecordsPage.getCurrent(), donationRecordsPage.getSize(), donationRecordsPage.getTotal());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = donationRecordsList.stream().map(DonationRecords::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        if (CollUtil.isEmpty(donationRecordsList)) {
            return donationRecordsVoPage;
        }
        // 填充信息
        List<DonationRecordsVO> donationRecordsVOList = donationRecordsList.stream().map(donationRecords -> {
            DonationRecordsVO donationRecordsVO = DonationRecordsVO.objToVo(donationRecords);
            Long userId = donationRecords.getUserId();
            User donorUser = null;
            if (userIdUserListMap.containsKey(userId)) {
                donorUser = userIdUserListMap.get(userId).get(0);
            }
            LoginUserVO loginUserVO = userService.getLoginUserVO(donorUser);
            if (loginUserVO != null){
                loginUserVO.setEmail(null);
            }

            donationRecordsVO.setDonorUser(loginUserVO);
            return donationRecordsVO;

        }).collect(Collectors.toList());
        donationRecordsVoPage.setRecords(donationRecordsVOList);
        return donationRecordsVoPage;

    }

    // endregion

}




