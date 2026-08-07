package com.cong.fishisland.game.landlords.util.poker;

import com.cong.fishisland.game.landlords.model.poker.Poker;
import com.cong.fishisland.game.landlords.model.poker.PokerHand;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 扑克牌排序器
 *
 * @author cong
 */
public class PokerSorter {

    private PokerSorter() {
    }

    /**
     * 按斗地主规则排序 (3最小, 王最大)
     */
    public static void sortByLandlords(PokerHand hand) {
        Collections.sort(hand.getPokers(), (a, b) -> b.getLandlordsSortValue() - a.getLandlordsSortValue());
    }

    /**
     * 按斗地主规则排序 (癞子牌排到最后)
     */
    public static void sortByLandlordsWithUniversal(PokerHand hand) {
        Collections.sort(hand.getPokers(), (a, b) -> {
            if (a.isUniversal() != b.isUniversal()) {
                return a.isUniversal() ? 1 : -1;
            }
            return b.getLandlordsSortValue() - a.getLandlordsSortValue();
        });
    }

    /**
     * 按花色和面值排序
     */
    public static void sortByTypeAndValue(PokerHand hand) {
        Collections.sort(hand.getPokers(), Comparator.comparingInt((Poker p) -> p.getType().getCode())
                .thenComparingInt(p -> p.getValue().getValue()));
    }

    /**
     * 获取排序后的牌值列表
     */
    public static List<Integer> getSortedValues(PokerHand hand) {
        return hand.getAll().stream()
                .map(Poker::getLandlordsSortValue)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    /**
     * 获取牌值出现次数 (用于分析牌型)
     */
    public static Map<Integer, Long> getValueCounts(PokerHand hand) {
        return hand.getAll().stream()
                .collect(Collectors.groupingBy(Poker::getLandlordsSortValue, Collectors.counting()));
    }

    /**
     * 获取牌值出现次数，按次数分组
     */
    public static Map<Integer, List<Integer>> getValueCountsGrouped(PokerHand hand) {
        Map<Integer, Long> counts = getValueCounts(hand);
        Map<Integer, List<Integer>> grouped = counts.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getValue().intValue(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));

        grouped.values().forEach(values -> Collections.sort(values, Collections.reverseOrder()));
        return grouped;
    }

    /**
     * 找出所有可能构成顺子的组合
     */
    public static List<List<Integer>> findStraights(PokerHand hand, int minLength) {
        Map<Integer, Long> counts = getValueCounts(hand);

        List<Integer> validValues = counts.keySet().stream()
                .filter(v -> v >= 3 && v <= 14)
                .sorted(Comparator.reverseOrder())
                .distinct()
                .collect(Collectors.toList());

        if (validValues.size() < minLength) {
            return new ArrayList<>();
        }

        Set<Integer> valueSet = new HashSet<>(validValues);
        List<List<Integer>> runs = new ArrayList<>();

        validValues.forEach(v -> extendRun(Arrays.asList(v), valueSet, minLength, runs));

        return runs;
    }

    private static void extendRun(List<Integer> current, Set<Integer> valueSet, int minLength, List<List<Integer>> runs) {
        int last = current.get(0);
        Integer next = last - 1;

        if (next >= 3 && valueSet.contains(next)) {
            List<Integer> extended = new ArrayList<>();
            extended.add(next);
            extended.addAll(current);
            extendRun(extended, valueSet, minLength, runs);
        } else {
            if (current.size() >= minLength) {
                runs.add(current);
            }
        }
    }
}
