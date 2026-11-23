package com.cong.fishisland.model.enums.farm;

import lombok.Getter;

/**
 * 农场 buff 道具类型
 * <p>效果数组下标 = 等级（0 位弃用），数值为百分比；收益递减，Lv5 即全局上限。
 * 数值为占位配置，最终以数值平衡为准（见 PRD 附录 A）。</p>
 */
@Getter
public enum FarmBuffTypeEnum {

    /** 生长加速光环：降低作物成熟总耗时 */
    GROWTH_ACCEL(1, "生长加速光环", "成熟时间", 9, 0, 5,
            new int[]{0, 10, 18, 24, 28, 30}, 200),

    /** 积分增产光环：收获积分按百分比提升 */
    YIELD_BOOST(2, "丰收光环", "积分产出", 10, 0, 5,
            new int[]{0, 10, 18, 24, 28, 35}, 200),

    /** 守护光环：降低被偷走的积分（按期望值折算为格挡概率，见 FarmConstants） */
    GUARD(3, "守护光环", "被偷减免", 11, 0, 5,
            new int[]{0, 12, 22, 30, 36, 42}, 300),

    /** 神奇生长光环：提升高品级图鉴与大重量出现几率 */
    LUCKY_GROWTH(4, "神奇生长光环", "高品级几率", 12, 25, 5,
            new int[]{0, 10, 20, 30, 40, 50}, 500);

    private final int type;
    private final String name;
    private final String effectName;
    /** 购买所需已解锁田地数 */
    private final int requiredLandCount;
    /** 购买所需农场等级 */
    private final int requiredFarmLevel;
    /** 等级上限 */
    private final int maxLevel;
    /** 各等级效果（百分比），下标 = 等级 */
    private final int[] levelEffects;
    /** 购买消耗积分（占位，待数值平衡） */
    private final int purchaseCost;

    FarmBuffTypeEnum(int type, String name, String effectName, int requiredLandCount,
                     int requiredFarmLevel, int maxLevel, int[] levelEffects, int purchaseCost) {
        this.type = type;
        this.name = name;
        this.effectName = effectName;
        this.requiredLandCount = requiredLandCount;
        this.requiredFarmLevel = requiredFarmLevel;
        this.maxLevel = maxLevel;
        this.levelEffects = levelEffects;
        this.purchaseCost = purchaseCost;
    }

    /**
     * 指定等级的效果百分比；等级越界返回 0。
     */
    public int effectAt(int level) {
        if (level < 1 || level >= levelEffects.length) {
            return 0;
        }
        return levelEffects[level];
    }

    /**
     * 效果描述，如"成熟时间 -18%"、"积分产出 +18%"。
     */
    public String effectDescription(int level) {
        String sign = (this == GROWTH_ACCEL || this == GUARD) ? "-" : "+";
        return effectName + " " + sign + effectAt(level) + "%";
    }

    public static FarmBuffTypeEnum fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (FarmBuffTypeEnum buffType : values()) {
            if (buffType.type == type) {
                return buffType;
            }
        }
        return null;
    }
}
