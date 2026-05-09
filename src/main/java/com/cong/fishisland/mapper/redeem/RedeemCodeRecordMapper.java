package com.cong.fishisland.mapper.redeem;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.redeem.RedeemCodeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 兑换码使用记录 Mapper
 *
 * @author cong
 */
@Mapper
public interface RedeemCodeRecordMapper extends BaseMapper<RedeemCodeRecord> {
}
