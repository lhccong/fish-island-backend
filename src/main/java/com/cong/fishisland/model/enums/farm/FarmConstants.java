package com.cong.fishisland.model.enums.farm;

/**
 * 农场业务常量
 */
public final class FarmConstants {

    private FarmConstants() {
    }

    /** 偷菜冷却时间（分钟） */
    public static final int STEAL_COOLDOWN_MINUTES = 10;

    /** 单株作物最多被偷次数 */
    public static final int MAX_STEAL_COUNT_PER_PLANT = 3;

    /** 每位用户地块总数 */
    public static final int LAND_TOTAL_COUNT = 9;

    /** 默认解锁地块数量 */
    public static final int LAND_DEFAULT_UNLOCKED_COUNT = 3;
}
