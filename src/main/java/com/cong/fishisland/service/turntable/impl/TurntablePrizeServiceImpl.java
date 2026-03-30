package com.cong.fishisland.service.turntable.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        QueryWrapper<TurntablePrize> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("turntableId", turntableId);
        queryWrapper.eq("isDelete", 0);
        return this.list(queryWrapper);
    }

    @Override
    public List<TurntablePrize> listAvailableByTurntableId(Long turntableId) {
        QueryWrapper<TurntablePrize> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("turntableId", turntableId);
        queryWrapper.eq("isDelete", 0);
        // 库存大于0或者库存为-1（无限）
        queryWrapper.and(wrapper -> wrapper.gt("stock", 0).or().eq("stock", -1));
        return this.list(queryWrapper);
    }
}
