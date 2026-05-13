package com.cong.fishisland.service.redeem.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.redeem.RedeemCodeRecordMapper;
import com.cong.fishisland.model.entity.redeem.RedeemCodeRecord;
import com.cong.fishisland.service.redeem.RedeemCodeRecordService;
import org.springframework.stereotype.Service;

/**
 * 兑换码使用记录 Service 实现
 *
 * @author cong
 */
@Service
public class RedeemCodeRecordServiceImpl extends ServiceImpl<RedeemCodeRecordMapper, RedeemCodeRecord>
        implements RedeemCodeRecordService {

    @Override
    public boolean hasRedeemed(Long codeId, Long userId) {
        return this.count(new LambdaQueryWrapper<RedeemCodeRecord>()
                .eq(RedeemCodeRecord::getCodeId, codeId)
                .eq(RedeemCodeRecord::getUserId, userId)) > 0;
    }
}
