package com.cong.fishisland.service.turntable.template;

import com.cong.fishisland.model.entity.turntable.TurntablePrize;
import com.cong.fishisland.model.vo.turntable.DrawPrizeVO;

import java.util.List;

/**
 * 抽奖模板方法抽象类
 * 定义抽奖流程的骨架：
 * 1. 抽奖前处理（扣积分...）
 * 2. 奖品抽取
 * 3. 抽奖记录的保存
 * 4. 修改用户的抽奖进度
 * 5. 奖励发放
 * @author cong
 */
public abstract class DrawTemplate {

    /**
     * 执行抽奖（模板方法）
     * @param userId 用户ID
     * @param turntableId 转盘ID
     * @param drawCount 抽奖次数
     * @return 抽奖结果列表
     */
    public List<DrawPrizeVO> executeDraw(Long userId, Long turntableId, int drawCount) {
        // 1. 抽奖前处理（校验、扣积分等）
        preDraw(userId, turntableId, drawCount);

        // 2. 执行奖品抽取
        List<DrawPrizeVO> prizes = doDraw(userId, turntableId, drawCount);

        // 3. 保存抽奖记录
        saveDrawRecords(userId, turntableId, prizes);

        // 4. 修改用户的抽奖进度
        updateUserProgress(userId, turntableId, prizes);

        // 5. 发放奖励
        deliverPrizes(userId, prizes);

        return prizes;
    }

    /**
     * 抽奖前处理（校验、扣积分等）
     */
    protected abstract void preDraw(Long userId, Long turntableId, int drawCount);

    /**
     * 执行奖品抽取
     */
    protected abstract List<DrawPrizeVO> doDraw(Long userId, Long turntableId, int drawCount);

    /**
     * 保存抽奖记录
     */
    protected abstract void saveDrawRecords(Long userId, Long turntableId, List<DrawPrizeVO> prizes);

    /**
     * 更新用户抽奖进度
     */
    protected abstract void updateUserProgress(Long userId, Long turntableId, List<DrawPrizeVO> prizes);

    /**
     * 发放奖励
     */
    protected abstract void deliverPrizes(Long userId, List<DrawPrizeVO> prizes);

    /**
     * 抽取单个奖品（由子类实现具体抽取逻辑）
     */
    protected abstract TurntablePrize drawSinglePrize(Long turntableId, boolean isGuarantee, int guaranteeType);
}
