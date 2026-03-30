package com.cong.fishisland.service.turntable.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.turntable.TurntablePrizeMapper;
import com.cong.fishisland.model.entity.turntable.TurntablePrize;
import com.cong.fishisland.service.turntable.TurntablePrizeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 转盘奖励服务实现
 * @author cong
 */
@Service
public class TurntablePrizeServiceImpl extends ServiceImpl<TurntablePrizeMapper, TurntablePrize> implements TurntablePrizeService {

    @Override
    public List<TurntablePrize> listByTurntableId(Long turntableId) {
        LambdaQueryWrapper<TurntablePrize> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TurntablePrize::getTurntableId, turntableId)
                .eq(TurntablePrize::getIsDelete, 0);
        return this.list(queryWrapper);
    }

    @Override
    public List<TurntablePrize> listAvailableByTurntableId(Long turntableId) {
        LambdaQueryWrapper<TurntablePrize> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TurntablePrize::getTurntableId, turntableId)
                .eq(TurntablePrize::getIsDelete, 0)
                // 库存大于0或者库存为-1（无限）
                .and(wrapper -> wrapper.gt(TurntablePrize::getStock, 0).or().eq(TurntablePrize::getStock, -1));
        return this.list(queryWrapper);
    }
}
