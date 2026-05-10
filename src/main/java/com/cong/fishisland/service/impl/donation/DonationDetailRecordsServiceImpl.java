package com.cong.fishisland.service.impl.donation;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.donation.DonationDetailRecordsMapper;
import com.cong.fishisland.model.dto.donation.DonationDetailRecordsQueryRequest;
import com.cong.fishisland.model.entity.donation.DonationDetailRecords;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.donation.DonationDetailRecordsVO;
import com.cong.fishisland.model.vo.user.LoginUserVO;
import com.cong.fishisland.service.DonationDetailRecordsService;
import com.cong.fishisland.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 打赏明细记录 Service 实现
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
@Slf4j
@Service
public class DonationDetailRecordsServiceImpl
        extends ServiceImpl<DonationDetailRecordsMapper, DonationDetailRecords>
        implements DonationDetailRecordsService {

    @Resource
    private UserService userService;

    @Override
    public void addDetail(Long userId, BigDecimal amount, String remark) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        ThrowUtils.throwIf(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0,
                ErrorCode.PARAMS_ERROR, "打赏金额必须大于0");

        DonationDetailRecords detail = new DonationDetailRecords();
        detail.setUserId(userId);
        detail.setAmount(amount);
        detail.setRemark(remark);
        boolean saved = this.save(detail);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存打赏明细失败");
        log.info("[DonationDetail] 打赏明细已记录，userId={}, amount={}", userId, amount);
    }

    @Override
    public Page<DonationDetailRecordsVO> listDetailVOByPage(DonationDetailRecordsQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        // 防爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR, "每页最多查询50条");

        LambdaQueryWrapper<DonationDetailRecords> wrapper = new LambdaQueryWrapper<DonationDetailRecords>()
                .eq(queryRequest.getUserId() != null, DonationDetailRecords::getUserId, queryRequest.getUserId())
                .orderByDesc(DonationDetailRecords::getCreateTime);

        Page<DonationDetailRecords> entityPage = this.page(new Page<>(current, size), wrapper);
        Page<DonationDetailRecordsVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());

        List<DonationDetailRecords> records = entityPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }

        // 批量查询用户信息
        Set<Long> userIdSet = records.stream().map(DonationDetailRecords::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<DonationDetailRecordsVO> voList = records.stream().map(detail -> {
            DonationDetailRecordsVO vo = DonationDetailRecordsVO.objToVo(detail);
            User user = userMap.get(detail.getUserId());
            LoginUserVO loginUserVO = userService.getLoginUserVO(user);
            if (loginUserVO != null) {
                loginUserVO.setEmail(null);
            }
            vo.setDonorUser(loginUserVO);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
