package com.cong.fishisland.service.turntable.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.turntable.TurntableUserProgressMapper;
import com.cong.fishisland.model.entity.turntable.TurntableUserProgress;
import com.cong.fishisland.model.enums.turntable.GuaranteeTypeEnum;
import com.cong.fishisland.service.turntable.TurntableUserProgressService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户转盘进度服务实现
 * @author cong
 */
@Service
public class TurntableUserProgressServiceImpl extends ServiceImpl<TurntableUserProgressMapper, TurntableUserProgress> implements TurntableUserProgressService {

    @Override
    public TurntableUserProgress getOrCreateProgress(Long userId, Long turntableId, Integer guaranteeCount) {
        LambdaQueryWrapper<TurntableUserProgress> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TurntableUserProgress::getUserId, userId)
                .eq(TurntableUserProgress::getTurntableId, turntableId)
                .eq(TurntableUserProgress::getIsDelete, 0);
        
        TurntableUserProgress progress = this.getOne(queryWrapper);
        
        if (progress == null) {
            progress = new TurntableUserProgress();
            progress.setUserId(userId);
            progress.setTurntableId(turntableId);
            progress.setSmallFailCount(0);
            progress.setTotalDrawCount(0);
            progress.setGuaranteeCount(guaranteeCount != null ? guaranteeCount : 300);
            progress.setLastDrawTime(new Date());
            this.save(progress);
        }
        
        return progress;
    }

    @Override
    public void updateProgress(Long userId, Long turntableId, boolean isGuaranteeHit, int guaranteeType, int drawCount) {
        // 传入 0 表示获取已有进度，不创建新进度
        TurntableUserProgress progress = this.getOrCreateProgress(userId, turntableId, 0);
        
        // 获取保底次数配置
        int guaranteeCount = progress.getGuaranteeCount() != null ? progress.getGuaranteeCount() : 0;
        
        // 更新累计抽奖次数
        progress.setTotalDrawCount(progress.getTotalDrawCount() + drawCount);
        
        // 更新小保底失败次数
        progress.setSmallFailCount(progress.getSmallFailCount() + drawCount);
        
        // 更新上次抽奖时间
        progress.setLastDrawTime(new Date());
        
        // 如果命中保底，重置相应计数
        if (isGuaranteeHit) {
            if (guaranteeType == GuaranteeTypeEnum.SMALL.getValue()) {
                // 小保底命中，清零小保底计数
                progress.setSmallFailCount(0);
            } else if (guaranteeType == GuaranteeTypeEnum.BIG.getValue()) {
                // 大保底命中，重置或减保底次数
                progress.setTotalDrawCount(Math.max(0, progress.getTotalDrawCount() - guaranteeCount));
                progress.setSmallFailCount(0);
            }
        }
        
        this.updateById(progress);
    }
}
