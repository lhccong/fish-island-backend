package com.cong.fishisland.model.enums.farm;

/**
 * 农场业务常量
 */
public final class FarmConstants {

    private FarmConstants() {
    }

    /** 单次偷菜最多获得积分 */
    public static final int MAX_STEAL_POINTS_PER_ACTION = 1;

    /** 每位用户地块总数 */
    public static final int LAND_TOTAL_COUNT = 24;

    /** 默认解锁地块数量 */
    public static final int LAND_DEFAULT_UNLOCKED_COUNT = 8;
}
