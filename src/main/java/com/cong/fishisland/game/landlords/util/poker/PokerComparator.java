package com.cong.fishisland.game.landlords.util.poker;

import com.cong.fishisland.game.landlords.enums.poker.PokerPatternEnum;
import com.cong.fishisland.game.landlords.model.poker.PatternResult;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 扑克牌型比较器
 *
 * @author cong
 */
public class PokerComparator {

    private PokerComparator() {
    }

    /**
     * 比较两个牌型的大小
     *
     * @return 正数表示 result1 > result2, 负数表示 result1 < result2, 0表示相等
     */
    public static int compare(PatternResult result1, PatternResult result2) {
        if (result1 == null || !result1.isValid()) {
            return result2 == null || !result2.isValid() ? 0 : -1;
        }
        if (result2 == null || !result2.isValid()) {
            return 1;
        }

        PokerPatternEnum pattern1 = result1.getPattern();
        PokerPatternEnum pattern2 = result2.getPattern();

        // 王炸最大
        if (pattern1 == PokerPatternEnum.JOKER_BOMB) {
            return pattern2 == PokerPatternEnum.JOKER_BOMB ? 0 : 1;
        }
        if (pattern2 == PokerPatternEnum.JOKER_BOMB) {
            return -1;
        }

        // 普通炸弹
        if (pattern1 == PokerPatternEnum.BOMB) {
            if (pattern2 == PokerPatternEnum.BOMB) {
                // 都是炸弹，比较面值
                return result1.getMainValue() - result2.getMainValue();
            }
            return 1; // 炸弹比其他牌型大
        }
        if (pattern2 == PokerPatternEnum.BOMB) {
            return -1;
        }

        // 牌型不同不能比较
        if (!isSamePatternType(pattern1, pattern2)) {
            return 0; // 无法比较
        }

        // 数量不同不能比较
        if (result1.getCount() != result2.getCount()) {
            return 0;
        }

        // 比较主牌面值
        return result1.getMainValue() - result2.getMainValue();
    }

    /**
     * 检查一个牌型是否能打过另一个牌型
     */
    public static boolean canBeat(PatternResult myPattern, PatternResult opponentPattern) {
        if (myPattern == null || !myPattern.isValid()) {
            return false;
        }
        if (opponentPattern == null || !opponentPattern.isValid()) {
            return true; // 首次出牌
        }

        return compare(myPattern, opponentPattern) > 0;
    }

    /**
     * 判断两个牌型是否同类型
     */
    public static boolean isSamePatternType(PokerPatternEnum pattern1, PokerPatternEnum pattern2) {
        // 单张
        if (pattern1 == PokerPatternEnum.SINGLE && pattern2 == PokerPatternEnum.SINGLE) {
            return true;
        }

        // 对子
        if (pattern1 == PokerPatternEnum.PAIR && pattern2 == PokerPatternEnum.PAIR) {
            return true;
        }

        // 三张
        if (pattern1 == PokerPatternEnum.TRIPLE && pattern2 == PokerPatternEnum.TRIPLE) {
            return true;
        }

        // 三带一
        if (pattern1 == PokerPatternEnum.TRIPLE_SINGLE && pattern2 == PokerPatternEnum.TRIPLE_SINGLE) {
            return true;
        }

        // 三带二
        if (pattern1 == PokerPatternEnum.TRIPLE_PAIR && pattern2 == PokerPatternEnum.TRIPLE_PAIR) {
            return true;
        }

        // 顺子
        if (pattern1 == PokerPatternEnum.STRAIGHT && pattern2 == PokerPatternEnum.STRAIGHT) {
            return true;
        }

        // 连对
        if (pattern1 == PokerPatternEnum.DOUBLE_STRAIGHT && pattern2 == PokerPatternEnum.DOUBLE_STRAIGHT) {
            return true;
        }

        // 飞机 (及其带牌变种)
        if (isPlaneType(pattern1) && isPlaneType(pattern2)) {
            return true;
        }

        return false;
    }

    /**
     * 是否是飞机类牌型
     */
    private static boolean isPlaneType(PokerPatternEnum pattern) {
        return pattern == PokerPatternEnum.PLANE ||
               pattern == PokerPatternEnum.PLANE_SINGLE ||
               pattern == PokerPatternEnum.PLANE_PAIR;
    }

    /**
     * 获取牌型描述
     */
    public static String getPatternDescription(PatternResult result) {
        if (result == null || !result.isValid()) {
            return "无效牌型";
        }

        PokerPatternEnum pattern = result.getPattern();
        int mainValue = result.getMainValue();
        int count = result.getCount();

        String valueStr = getValueString(mainValue);

        switch (pattern) {
            case SINGLE:
                return "单张 " + valueStr;
            case PAIR:
                return "对子 " + valueStr;
            case TRIPLE:
                return "三张 " + valueStr;
            case TRIPLE_SINGLE:
                return "三带一(" + valueStr + ")";
            case TRIPLE_PAIR:
                return "三带二(" + valueStr + ")";
            case BOMB:
                return "炸弹 " + valueStr;
            case JOKER_BOMB:
                return "王炸";
            case STRAIGHT:
                return count + "张顺子";
            case DOUBLE_STRAIGHT:
                return (count / 2) + "对连对";
            case PLANE:
                return (count / 3) + "个飞机";
            case PLANE_SINGLE:
                return (count / 4) + "个飞机带单";
            case PLANE_PAIR:
                return (count / 5) + "个飞机带对";
            default:
                return pattern.getName();
        }
    }

    /**
     * 将面值转换为可读字符串
     */
    public static String getValueString(int value) {
        switch (value) {
            case 3: case 4: case 5: case 6: case 7: case 8: case 9:
            case 10:
                return String.valueOf(value);
            case 11:
                return "J";
            case 12:
                return "Q";
            case 13:
                return "K";
            case 14:
                return "A";
            case 15:
                return "2";
            case 16:
                return "小王";
            case 17:
                return "大王";
            default:
                return String.valueOf(value);
        }
    }

    /**
     * 比较结果
     */
    @Data
    @AllArgsConstructor
    public static class CompareResult {
        private boolean canBeat;
        private String reason;

        public static CompareResult win() {
            return new CompareResult(true, "可以打过");
        }

        public static CompareResult lose(String reason) {
            return new CompareResult(false, reason);
        }
    }
}
