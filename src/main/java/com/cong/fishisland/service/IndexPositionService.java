package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.fund.IndexPosition;

import java.math.BigDecimal;

/**
 * 指数持仓服务
 * 
 * 职责：管理用户指数持仓的增删改查，包括份额锁定/解锁、可用份额扣减等核心操作
 * 
 * @author shing
 */
public interface IndexPositionService extends IService<IndexPosition> {

    // ==================== 持仓查询 ====================

    /**
     * 获取或创建用户持仓记录
     * 如果持仓不存在，自动创建初始化记录
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @return 持仓记录
     */
    IndexPosition getOrCreatePosition(Long userId, String indexCode);

    /**
     * 获取用户可用份额
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @return 可用份额
     */
    BigDecimal getAvailableShares(Long userId, String indexCode);

    // ==================== 买入相关 ====================

    /**
     * 买入增加锁定份额（T+1 可卖规则）
     * 
     * 操作：
     * - totalShares += shares
     * - lockedShares += shares
     * - 更新加权平均成本
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @param shares 买入份额
     * @param nav 买入净值
     */
    void addLockedShares(Long userId, String indexCode, BigDecimal shares, BigDecimal nav);

    // ==================== 卖出相关 ====================

    /**
     * 卖出扣减可用份额（T+0 实时到账）
     * 
     * 操作（原子 CAS）：
     * - 检查 availableShares >= shares
     * - totalShares -= shares
     * - availableShares -= shares
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @param shares 卖出份额
     * @return true-成功，false-可用份额不足
     */
    boolean reduceAvailableShares(Long userId, String indexCode, BigDecimal shares);

    // ==================== 份额解锁 ====================

    /**
     * 批量解锁所有用户的锁定份额（定时任务调用）
     * 
     * 操作：
     * - availableShares += lockedShares
     * - lockedShares = 0
     * 
     * 执行时机：每日 09:30
     */
    void unlockAllLockedShares();

    // ==================== 盈亏计算 ====================

    /**
     * 计算用户持仓浮动盈亏
     * 
     * @param userId 用户ID
     * @param indexCode 指数代码
     * @param currentNav 当前净值
     * @return 盈亏金额（积分）
     */
    Long calculateProfitLoss(Long userId, String indexCode, BigDecimal currentNav);
}
