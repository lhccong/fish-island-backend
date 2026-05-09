package com.cong.fishisland.service.redeem;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.redeem.RedeemCodeRecord;

/**
 * 兑换码使用记录 Service
 *
 * @author cong
 */
public interface RedeemCodeRecordService extends IService<RedeemCodeRecord> {

    /**
     * 判断用户是否已兑换过该兑换码
     *
     * @param codeId 兑换码ID
     * @param userId 用户ID
     * @return 是否已兑换
     */
    boolean hasRedeemed(Long codeId, Long userId);
}
