package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.donation.DonationDetailRecordsQueryRequest;
import com.cong.fishisland.model.entity.donation.DonationDetailRecords;
import com.cong.fishisland.model.vo.donation.DonationDetailRecordsVO;

import java.math.BigDecimal;

/**
 * 打赏明细记录 Service
 *
 * @author <a href="https://github.com/lhccong">程序员聪</a>
 */
public interface DonationDetailRecordsService extends IService<DonationDetailRecords> {

    /**
     * 新增一条打赏明细（每次打赏独立插入，不累加）
     *
     * @param userId 打赏用户ID
     * @param amount 本次打赏金额
     * @param remark 打赏留言
     */
    void addDetail(Long userId, BigDecimal amount, String remark);

    /**
     * 分页查询打赏明细列表（带用户信息，按打赏时间倒序）
     *
     * @param queryRequest 查询请求
     * @return 分页 VO
     */
    Page<DonationDetailRecordsVO> listDetailVOByPage(DonationDetailRecordsQueryRequest queryRequest);
}
