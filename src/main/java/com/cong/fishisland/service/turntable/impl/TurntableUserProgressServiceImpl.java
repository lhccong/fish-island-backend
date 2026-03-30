package com.cong.fishisland.service.turntable.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.turntable.TurntableUserProgressMapper;
import com.cong.fishisland.model.entity.turntable.TurntableUserProgress;
import com.cong.fishisland.service.turntable.TurntableUserProgressService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户转盘进度服务实现
 * @author cong
 */
@Service
public class TurntableUserProgressServiceImpl extends ServiceImpl<TurntableUserProgressMapper, TurntableUserProgress> implements TurntableUserProgressService {

    /**
     * 小保底触发次数
     */
    private static final int SMALL_GUARANTEE_COUNT = 10;

    /**
     * 大保底触发次数
     */
    private static final int BIG_GUARANTEE_COUNT = 300;

    @Override
    public TurntableUserProgress getOrCreateProgress(Long userId, Long turntableId, Integer guaranteeCount) {
        QueryWrapper<TurntableUserProgress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("turntableId", turntableId);
        queryWrapper.eq("isDelete", 0);
        
        TurntableUserProgress progress = this.getOne(queryWrapper);
        
        if (progress == null) {
            progress = new TurntableUserProgress();
            progress.setUserId(userId);
            progress.setTurntableId(turntableId);
            progress.setSmallFailCount(0);
            progress.setTotalDrawCount(0);
            progress.setGuaranteeCount(guaranteeCount != null ? guaranteeCount : BIG_GUARANTEE_COUNT);
            progress.setLastDrawTime(new Date());
            this.save(progress);
        }
        
        return progress;
    }

    @Override
    public void updateProgress(Long userId, Long turntableId, boolean isGuaranteeHit, int guaranteeType, int drawCount) {
        TurntableUserProgress progress = this.getOrCreateProgress(userId, turntableId, BIG_GUARANTEE_COUNT);
        
        // 更新累计抽奖次数
        progress.setTotalDrawCount(progress.getTotalDrawCount() + drawCount);
        
        // 更新小保底失败次数
        progress.setSmallFailCount(progress.getSmallFailCount() + drawCount);
        
        // 更新上次抽奖时间
        progress.setLastDrawTime(new Date());
        
        // 如果命中保底，重置相应计数
        if (isGuaranteeHit) {
            if (guaranteeType == 1) {
                // 小保底命中，清零小保底计数
                progress.setSmallFailCount(0);
            } else if (guaranteeType == 2) {
                // 大保底命中，重置或减300
                progress.setTotalDrawCount(Math.max(0, progress.getTotalDrawCount() - BIG_GUARANTEE_COUNT));
                progress.setSmallFailCount(0);
            }
        }
        
        this.updateById(progress);
    }
}
