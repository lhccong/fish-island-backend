package com.cong.fishisland.mapper.fund;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cong.fishisland.model.entity.fund.IndexPosition;

/**
 * 指数持仓 Mapper
 */
public interface IndexPositionMapper extends BaseMapper<IndexPosition> {

    /**
     * 批量解锁所有持仓的 lockedShares
     */
    int unlockAllLockedShares();
}
