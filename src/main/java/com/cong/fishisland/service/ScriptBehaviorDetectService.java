package com.cong.fishisland.service;

/**
 * 脚本行为检测服务
 *
 * @author cong
 */
public interface ScriptBehaviorDetectService {

    /**
     * 是否已被标记为脚本用户
     */
    boolean isScriptUser(Long userId);

    /**
     * 固定间隔检测：记录用户每次操作的时间戳，
     * 若最近 N 次的相邻间隔标准差极小，视为脚本行为并标记
     *
     * @param userId       用户ID
     * @param tsKeyPrefix  时间戳 Redis key 前缀（不含 userId）
     * @param actionLabel  行为名称，用于日志和通知
     */
    void checkFixedIntervalBehavior(Long userId, String tsKeyPrefix, String actionLabel);

    /**
     * 标记用户为脚本用户并通知管理员
     */
    void markAsScriptUser(Long userId, String reason);

    /**
     * 手动标记或取消标记用户为脚本用户
     *
     * @param userId 目标用户ID
     * @param mark   true=标记为脚本，false=取消标记
     */
    void markScriptUser(Long userId, boolean mark);
}
