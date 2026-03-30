package com.cong.fishisland.service.turntable.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.turntable.TurntableDrawRecordMapper;
import com.cong.fishisland.model.entity.turntable.TurntableDrawRecord;
import com.cong.fishisland.service.turntable.TurntableDrawRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 转盘抽奖记录服务实现
 * @author cong
 */
@Service
public class TurntableDrawRecordServiceImpl extends ServiceImpl<TurntableDrawRecordMapper, TurntableDrawRecord> implements TurntableDrawRecordService {

    @Override
    public void saveBatchRecords(List<TurntableDrawRecord> records) {
        this.saveBatch(records);
    }

    @Override
    public List<TurntableDrawRecord> listByUserIdAndTurntableId(Long userId, Long turntableId) {
        LambdaQueryWrapper<TurntableDrawRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TurntableDrawRecord::getUserId, userId)
                .eq(TurntableDrawRecord::getTurntableId, turntableId)
                .eq(TurntableDrawRecord::getIsDelete, 0)
                .orderByDesc(TurntableDrawRecord::getCreateTime);
        return this.list(queryWrapper);
    }
}
