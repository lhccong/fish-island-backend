package com.cong.fishisland.service.turntable;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.turntable.TurntableUserProgress;

/**
 * 用户转盘进度服务接口
 * @author cong
 */
public interface TurntableUserProgressService extends IService<TurntableUserProgress> {

    /**
     * 获取或创建用户进度
     * @param userId 用户ID
     * @param turntableId 转盘ID
     * @param guaranteeCount 保底阈值
     * @return 用户进度
     */
    TurntableUserProgress getOrCreateProgress(Long userId, Long turntableId, Integer guaranteeCount);

    /**
     * 更新用户进度
     * @param userId 用户ID
     * @param turntableId 转盘ID
     * @param isGuaranteeHit 是否命中保底
     * @param guaranteeType 保底类型 1-小保底 2-大保底
     * @param drawCount 本次抽奖次数
     */
    void updateProgress(Long userId, Long turntableId, boolean isGuaranteeHit, int guaranteeType, int drawCount);
}
