package com.cong.fishisland.model.enums.farm;

/**
 * 农场业务常量
 */
public final class FarmConstants {

    private FarmConstants() {
    }

    /** 单次偷菜最多获得积分 */
    public static final int MAX_STEAL_POINTS_PER_ACTION = 1;

    /** 农场主收获时，在种子价格基础上额外保留的积分 */
    public static final int MIN_HARVEST_EXTRA_OVER_SEED = 1;

    /**
     * 农场主收获最低积分（种子价格 + {@link #MIN_HARVEST_EXTRA_OVER_SEED}），也是偷菜后必须保留的积分。
     */
    public static int minHarvestPoints(Integer seedPrice) {
        return (seedPrice != null ? seedPrice : 0) + MIN_HARVEST_EXTRA_OVER_SEED;
    }

    /** 偷菜记录列表固定返回条数 */
    public static final int STEAL_RECORD_LIST_LIMIT = 50;

    /** 每位用户地块总数 */
    public static final int LAND_TOTAL_COUNT = 24;

    /** 默认解锁地块数量 */
    public static final int LAND_DEFAULT_UNLOCKED_COUNT = 8;

    /**
     * 当前可通过农场等级解锁的最大地块序号。
     * 第 1–8 块默认解锁；第 9–12 块按等级解锁；第 13 块及以后暂未开放。
     */
    public static final int LAND_MAX_LEVEL_UNLOCK_INDEX = 12;

    /**
     * 各地块解锁所需农场等级（下标 = landIndex）。
     * <ul>
     *   <li>1–8：1 级（默认解锁）</li>
     *   <li>9：10 级</li>
     *   <li>10：25 级</li>
     *   <li>11：40 级</li>
     *   <li>12：55 级（当前满级）</li>
     * </ul>
     */
    private static final int[] LAND_UNLOCK_LEVEL_BY_INDEX = {
            0,
            1, 1, 1, 1, 1, 1, 1, 1,  // 1–8
            10,                        // 9
            25,                        // 10
            40,                        // 11
            55                         // 12
    };

    /**
     * 各地块解锁所需可用积分（下标 = landIndex）。
     * <ul>
     *   <li>1–8：0（默认解锁，不消耗）</li>
     *   <li>9：100</li>
     *   <li>10：400</li>
     *   <li>11：1000</li>
     *   <li>12：2000</li>
     * </ul>
     */
    private static final int[] LAND_UNLOCK_COST_BY_INDEX = {
            0,
            0, 0, 0, 0, 0, 0, 0, 0,  // 1–8
            100,                      // 9
            400,                      // 10
            1000,                     // 11
            2000                      // 12
    };

    /**
     * 查询指定地块序号解锁所需的农场等级。
     *
     * @param landIndex 地块序号（1 起）
     * @return 所需等级；超出当前可解锁范围时返回 {@link Integer#MAX_VALUE}
     */
    public static int unlockLevelForLandIndex(int landIndex) {
        if (landIndex < 1 || landIndex > LAND_MAX_LEVEL_UNLOCK_INDEX) {
            return Integer.MAX_VALUE;
        }
        return LAND_UNLOCK_LEVEL_BY_INDEX[landIndex];
    }

    /**
     * 查询指定地块序号解锁所需的可用积分。
     *
     * @param landIndex 地块序号（1 起）
     * @return 所需积分；默认解锁或超出范围时返回 0 / {@link Integer#MAX_VALUE}
     */
    public static int unlockCostForLandIndex(int landIndex) {
        if (landIndex < 1 || landIndex > LAND_MAX_LEVEL_UNLOCK_INDEX) {
            return Integer.MAX_VALUE;
        }
        return LAND_UNLOCK_COST_BY_INDEX[landIndex];
    }

    /**
     * 判断指定地块是否已达到可按等级解锁的范围（含默认解锁的 1–8）。
     */
    public static boolean isLevelUnlockableLandIndex(int landIndex) {
        return landIndex >= 1 && landIndex <= LAND_MAX_LEVEL_UNLOCK_INDEX;
    }

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

    // ==================== 农场 buff 道具 & 作物图鉴（PRD v2.1） ====================

    /** buff 升级所需积分（下标 = 目标等级），占位数值待平衡 */
    public static final int[] BUFF_UPGRADE_COST = {0, 0, 10, 20, 30, 40};

    /** buff 升到各等级的一次性里程碑奖励（下标 = 等级）：每级都有，首级 100，最高 500 */
    public static final int[] BUFF_LEVEL_UP_REWARD = {0, 100, 200, 300, 400, 500};

    /** 每 5 级持续任务的基础奖励，实际奖励 = 基础值 × 档位（level / 5） */
    public static final int FARM_LEVEL_REWARD_BASE = 50;

    /** 持续任务奖励档位间隔 */
    public static final int FARM_LEVEL_REWARD_STEP = 5;

    /**
     * 指定等级的持续任务奖励（等级为 5 的倍数时有效）。
     */
    public static int levelRewardFor(int level) {
        if (level <= 0 || level % FARM_LEVEL_REWARD_STEP != 0) {
            return 0;
        }
        return FARM_LEVEL_REWARD_BASE * (level / FARM_LEVEL_REWARD_STEP);
    }

    /** 图鉴品级重量乘数曲线（下标 = 品级-1），占位数值待平衡 */
    private static final double[] GRADE_WEIGHT_MULTIPLIER = {
            1.0, 1.4, 1.9, 2.5, 3.2, 4.0, 4.9, 5.9, 7.0, 8.2
    };

    /** 品级内重量随机浮动幅度（±比例） */
    public static final double GRADE_WEIGHT_FLUCTUATION = 0.10;

    /**
     * 品级重量乘数（1–10 级，越高级越重）。
     */
    public static double weightMultiplier(int grade) {
        if (grade < 1 || grade > GRADE_WEIGHT_MULTIPLIER.length) {
            return 1.0;
        }
        return GRADE_WEIGHT_MULTIPLIER[grade - 1];
    }

    /** 图鉴品级默认概率权重（下标 = 品级-1，总和 1000），作物未配置 gradeWeightJson 时使用 */
    public static final int[] DEFAULT_GRADE_WEIGHTS = {300, 240, 170, 110, 70, 45, 30, 20, 12, 3};

    /** 图鉴品级上限 */
    public static final int COLLECTION_GRADE_MAX = 10;

    /** 神奇生长光环加成生效的最低品级（低于该品级的概率权重不放大） */
    public static final int LUCKY_BOOST_MIN_GRADE = 5;
}
