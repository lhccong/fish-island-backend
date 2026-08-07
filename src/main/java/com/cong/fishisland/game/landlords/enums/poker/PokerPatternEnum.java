package com.cong.fishisland.game.landlords.enums.poker;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 扑克牌型枚举
 *
 * @author cong
 */
@Getter
@AllArgsConstructor
public enum PokerPatternEnum {
    
    // 单牌类
    SINGLE(1, "单张", "任意一张牌"),
    
    // 对子类
    PAIR(2, "对子", "两张点数相同的牌"),
    PURE_PAIR(201, "姐妹对", "两个连续的对子"),
    
    // 三牌类
    TRIPLE(3, "三张", "三张点数相同的牌"),
    TRIPLE_SINGLE(4, "三带一", "三张+任意单张"),
    TRIPLE_PAIR(5, "三带二", "三张+一对"),
    
    // 四牌类
    BOMB(6, "炸弹", "四张点数相同的牌"),
    QUAD_PLANE(7, "四代二", "四张+两张单牌"),
    QUAD_TWO_PAIRS(8, "四代两对", "四张+两对"),
    BOMB_PAIR(9, "航天飞机", "四张+两对"),
    
    // 顺子类
    STRAIGHT(10, "顺子", "5张或更多连续的点数的单牌"),
    DOUBLE_STRAIGHT(11, "连对", "3对或更多连续点数的对子"),
    PLANE(12, "飞机", "两个或更多连续点数的三张"),
    PLANE_SINGLE(13, "飞机带单", "飞机+同等数量的单牌"),
    PLANE_PAIR(14, "飞机带对", "飞机+同等数量的对子"),
    
    // 王炸
    JOKER_BOMB(15, "王炸", "大小王组成"),
    
    // 无效
    INVALID(0, "无效", "无效牌型");

    private final int code;
    private final String name;
    private final String description;

    public static PokerPatternEnum getByCode(int code) {
        for (PokerPatternEnum pattern : values()) {
            if (pattern.code == code) {
                return pattern;
            }
        }
        return INVALID;
    }

    /**
     * 是否是炸弹 (包括王炸)
     */
    public boolean isBomb() {
        return this == BOMB || this == JOKER_BOMB;
    }

    /**
     * 获取牌型优先级 (用于比较)
     */
    public int getPriority() {
        switch (this) {
            case JOKER_BOMB:
                return 100;
            case BOMB:
                return 50;
            case QUAD_TWO_PAIRS:
            case QUAD_PLANE:
            case PLANE_PAIR:
            case PLANE_SINGLE:
            case PLANE:
            case DOUBLE_STRAIGHT:
            case STRAIGHT:
                return 30;
            case TRIPLE_PAIR:
            case TRIPLE_SINGLE:
            case TRIPLE:
                return 20;
            case PURE_PAIR:
            case PAIR:
                return 10;
            default:
                return 1;
        }
    }
}
