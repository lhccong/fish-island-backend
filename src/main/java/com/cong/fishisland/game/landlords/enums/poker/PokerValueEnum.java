package com.cong.fishisland.game.landlords.enums.poker;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 扑克牌面值枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum PokerValueEnum {
    
    THREE(3, "3", "3", 3),
    FOUR(4, "4", "4", 4),
    FIVE(5, "5", "5", 5),
    SIX(6, "6", "6", 6),
    SEVEN(7, "7", "7", 7),
    EIGHT(8, "8", "8", 8),
    NINE(9, "9", "9", 9),
    TEN(10, "10", "10", 10),
    JACK(11, "J", "J", 11),
    QUEEN(12, "Q", "Q", 12),
    KING(13, "K", "K", 13),
    ACE(14, "A", "A", 14),
    TWO(15, "2", "2", 15),
    SMALL_JOKER(16, "s", "小王", 16),
    BIG_JOKER(17, "x", "大王", 17);

    private final int code;
    private final String alias;
    private final String display;
    private final int value;

    /**
     * 根据 code 获取枚举
     */
    public static PokerValueEnum getByCode(int code) {
        for (PokerValueEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据别名获取枚举 (用于解析用户输入)
     * 忽略大小写，避免别名里 J/Q/K/A 大小写导致解析失败
     */
    public static PokerValueEnum getByAlias(String alias) {
        if (alias == null || alias.isEmpty()) {
            return null;
        }
        String lower = alias.toLowerCase();
        for (PokerValueEnum value : values()) {
            if (value.alias.equalsIgnoreCase(lower)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 是否是王
     */
    public boolean isJoker() {
        return this == SMALL_JOKER || this == BIG_JOKER;
    }

    /**
     * 是否是大王
     */
    public boolean isBigJoker() {
        return this == BIG_JOKER;
    }

    /**
     * 是否是小王
     */
    public boolean isSmallJoker() {
        return this == SMALL_JOKER;
    }

    /**
     * 是否是2
     */
    public boolean isTwo() {
        return this == TWO;
    }

    /**
     * 获取斗地主中的排序值 (3最小, 2次之, 王最大)
     */
    public int getLandlordsOrder() {
        return this.value;
    }

    /**
     * 是否在顺子/连对范围内 (3-A, 不含2和王)
     */
    public boolean canBeInStraight() {
        return this.value >= THREE.value && this.value <= ACE.value;
    }

    /**
     * 获取下一个面值 (顺子用)
     */
    public PokerValueEnum next() {
        if (this == THREE) {
            return FOUR;
        } else if (this == TEN) {
            return JACK;
        } else if (this == QUEEN) {
            return KING;
        } else if (this == KING) {
            return ACE;
        } else {
            return getByCode(this.value + 1);
        }
    }
}
