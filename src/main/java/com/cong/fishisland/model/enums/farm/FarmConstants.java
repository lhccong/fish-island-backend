package com.cong.fishisland.model.enums.farm;

/**
 * 农场业务常量
 */
public final class FarmConstants {

    private FarmConstants() {
    }

    /** 单次偷菜最多获得积分 */
    public static final int MAX_STEAL_POINTS_PER_ACTION = 1;

    /** 偷菜记录列表固定返回条数 */
    public static final int STEAL_RECORD_LIST_LIMIT = 50;

    /** 每位用户地块总数 */
    public static final int LAND_TOTAL_COUNT = 24;

    /** 默认解锁地块数量 */
    public static final int LAND_DEFAULT_UNLOCKED_COUNT = 8;

    /**
     * 从 1 级升到 2 级所需经验（单级基础值）。
     * 之后每升一级在此基础上递增 {@link #LEVEL_EXP_INCREMENT}。
     */
    public static final int LEVEL_EXP_BASE = 40;

    /** 每升一级，所需经验在上一级基础上增加的额度 */
    public static final int LEVEL_EXP_INCREMENT = 20;

    /**
     * 达到指定等级所需的累计经验（1 级为 0）。
     * <p>
     * 例如：1→2 需 40，2→3 需 60，3→4 需 80……
     */
    public static int cumulativeExpForLevel(int level) {
        if (level <= 1) {
            return 0;
        }
        int steps = level - 1;
        return steps * LEVEL_EXP_BASE + LEVEL_EXP_INCREMENT * steps * (steps - 1) / 2;
    }

    /**
     * 从当前等级升到下一级所需经验。
     */
    public static int expRequiredToAdvance(int currentLevel) {
        if (currentLevel < 1) {
            return LEVEL_EXP_BASE;
        }
        return LEVEL_EXP_BASE + (currentLevel - 1) * LEVEL_EXP_INCREMENT;
    }

    /**
     * 升到下一级还需要的经验。
     */
    public static int expToNextLevel(int currentLevel, int currentExp) {
        int nextThreshold = cumulativeExpForLevel(currentLevel + 1);
        return Math.max(0, nextThreshold - currentExp);
    }

    /**
     * 根据累计经验计算农场等级。
     */
    public static int calculateLevel(Integer experience) {
        if (experience == null || experience < 0) {
            return 1;
        }
        int level = 1;
        while (cumulativeExpForLevel(level + 1) <= experience) {
            level++;
        }
        return level;
    }
}
