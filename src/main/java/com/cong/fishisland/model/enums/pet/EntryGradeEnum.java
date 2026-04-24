package com.cong.fishisland.model.enums.pet;

import lombok.Getter;

/**
 * 装备词条等级枚举
 * 等级越高属性强度越高，但刷出概率越低
 *
 * @author cong
 */
@Getter
public enum EntryGradeEnum {

    /**
     * 白色 - 基础属性，概率最高
     * 基础属性倍率 1.0x，出现概率 50%
     */
    WHITE(1, "白", 1.0, 50),

    /**
     * 蓝色 - 较好属性，概率较高
     * 基础属性倍率 1.5x，出现概率 25%
     */
    BLUE(2, "蓝", 1.5, 25),

    /**
     * 紫色 - 优秀属性，概率中等
     * 基础属性倍率 2.0x，出现概率 15%
     */
    PURPLE(3, "紫", 2.0, 15),

    /**
     * 金色 - 稀有属性，概率较低
     * 基础属性倍率 3.0x，出现概率 8%
     */
    GOLD(4, "金", 3.0, 8),

    /**
     * 红色 - 传说属性，概率极低
     * 基础属性倍率 5.0x，出现概率 2%
     */
    RED(5, "红", 5.0, 2);

    private final int level;
    private final String label;
    /** 属性强度倍率 */
    private final double multiplier;
    /** 出现概率（百分比权重） */
    private final int weight;

    EntryGradeEnum(int level, String label, double multiplier, int weight) {
        this.level = level;
        this.label = label;
        this.multiplier = multiplier;
        this.weight = weight;
    }

    public static EntryGradeEnum of(int level) {
        for (EntryGradeEnum e : values()) {
            if (e.level == level) return e;
        }
        throw new IllegalArgumentException("未知词条等级: " + level);
    }
}
