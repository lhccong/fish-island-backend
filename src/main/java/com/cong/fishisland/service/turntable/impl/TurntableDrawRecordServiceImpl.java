package com.cong.fishisland.service.turntable.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        QueryWrapper<TurntableDrawRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("turntableId", turntableId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");
        return this.list(queryWrapper);
    }
}
