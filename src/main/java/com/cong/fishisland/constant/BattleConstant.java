package com.cong.fishisland.constant;

/**
 * 对战常量
 *
 * @author cong
 */
public interface BattleConstant {

    /**
     * 暴击伤害倍数
     */
    double CRITICAL_DAMAGE_MULTIPLIER = 2.0;

    /**
     * 连击伤害倍数
     */
    double COMBO_DAMAGE_MULTIPLIER = 1.5;

    /**
     * 攻击力基础值
     */
    int BASE_ATK = 10;

    /**
     * 攻击力成长率（指数成长）
     */
    double GROWTH_RATE = 0.06;

    /**
     * 格挡减伤比例（触发格挡时伤害乘以该系数）
     */
    double BLOCK_DAMAGE_REDUCTION = 0.5;
}
