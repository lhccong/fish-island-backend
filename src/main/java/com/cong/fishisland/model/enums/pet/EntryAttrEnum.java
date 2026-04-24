package com.cong.fishisland.model.enums.pet;

import lombok.Getter;

/**
 * 装备词条属性枚举
 * 每个属性按词条等级（白/蓝/紫/金/红）定义数值区间 [min, max]
 * 整数类属性（攻击/生命/防御）单位为点数
 * 百分比类属性单位为 %，保留两位小数
 *
 * @author cong
 */
@Getter
public enum EntryAttrEnum {

    // ===== 整数类属性 =====
    ATTACK("attack", "攻击力", false,
            new double[][]{{10, 30}, {31, 60}, {61, 100}, {101, 160}, {161, 250}}),

    MAX_HP("maxHp", "最大生命值", false,
            new double[][]{{50, 120}, {121, 250}, {251, 420}, {421, 650}, {651, 1000}}),

    DEFENSE("defense", "防御力", false,
            new double[][]{{8, 20}, {21, 45}, {46, 80}, {81, 130}, {131, 200}}),

    // ===== 百分比类属性 =====
    CRIT_RATE("critRate", "暴击率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    COMBO_RATE("comboRate", "连击率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    DODGE_RATE("dodgeRate", "闪避率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    BLOCK_RATE("blockRate", "格挡率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    LIFESTEAL("lifesteal", "吸血率", true,
            new double[][]{{0.3, 1.0}, {1.1, 2.0}, {2.1, 3.5}, {3.6, 5.5}, {5.6, 8.0}}),

    // ===== 抗性属性（百分比） =====
    ANTI_CRIT("antiCrit", "抗暴击率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    ANTI_COMBO("antiCombo", "抗连击率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    ANTI_DODGE("antiDodge", "抗闪避率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    ANTI_BLOCK("antiBlock", "抗格挡率", true,
            new double[][]{{0.5, 1.5}, {1.6, 3.0}, {3.1, 5.0}, {5.1, 8.0}, {8.1, 12.0}}),

    ANTI_LIFESTEAL("antiLifesteal", "抗吸血率", true,
            new double[][]{{0.3, 1.0}, {1.1, 2.0}, {2.1, 3.5}, {3.6, 5.5}, {5.6, 8.0}});

    private final String value;
    private final String label;
    /** true=百分比属性，false=整数属性 */
    private final boolean percentage;
    /**
     * 各等级数值区间，索引 0~4 对应白/蓝/紫/金/红
     * [min, max]
     */
    private final double[][] ranges;

    EntryAttrEnum(String value, String label, boolean percentage, double[][] ranges) {
        this.value = value;
        this.label = label;
        this.percentage = percentage;
        this.ranges = ranges;
    }

    /**
     * 获取指定等级的数值区间
     *
     * @param gradeLevel EntryGradeEnum.level（1~5）
     * @return [min, max]
     */
    public double[] getRangeByGrade(int gradeLevel) {
        return ranges[gradeLevel - 1];
    }

    public static EntryAttrEnum of(String value) {
        for (EntryAttrEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        throw new IllegalArgumentException("未知词条属性: " + value);
    }
}
