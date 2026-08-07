package com.cong.fishisland.game.landlords.service;

import com.cong.fishisland.game.landlords.enums.poker.PokerPatternEnum;
import com.cong.fishisland.game.landlords.enums.poker.PokerValueEnum;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import com.cong.fishisland.game.landlords.model.poker.PokerHand;
import com.cong.fishisland.game.landlords.model.poker.PatternResult;
import com.cong.fishisland.game.model.room.GameRoom;
import com.cong.fishisland.game.landlords.util.poker.PokerPatternMatcher;
import com.cong.fishisland.game.landlords.util.poker.PokerSorter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 斗地主AI托管服务
 * 负责处理AI托管时的自动操作
 *
 * @author cong
 */
@Slf4j
@Service
public class LandlordsRobotService {

    /**
     * AI叫分策略：始终不叫
     */
    public int getRobScore() {
        return 0;
    }

    /**
     * AI出牌策略
     * 规则：
     * 1. 如果上家没出牌或轮到自己先出，出最小单张
     * 2. 如果上家出过牌，能压就压最小的能压的牌，压不了就跳过
     *
     * @param room      房间
     * @param playerId  玩家ID
     * @return 要出的牌ID列表，如果选择跳过返回空列表
     */
    public List<String> getPlayCards(GameRoom room, Long playerId) {
        PokerHand hand = room.getPlayer(playerId).getHand();
        PokerHand lastPlayedCards = room.getLastPlayedCards();
        boolean isFirstPlay = room.getLastPlayerId() == null || room.getLastPlayerId().equals(playerId);

        // 按斗地主规则排序手牌
        PokerHand sortedHand = new PokerHand(hand.getAll());
        PokerSorter.sortByLandlordsWithUniversal(sortedHand);

        if (isFirstPlay || lastPlayedCards == null || lastPlayedCards.isEmpty()) {
            // 第一个出牌，出最小单张
            return playSmallestSingle(sortedHand);
        } else {
            // 有上家出过牌，尝试压牌
            PatternResult lastPattern = PokerPatternMatcher.analyze(lastPlayedCards);
            return playToBeat(sortedHand, lastPattern);
        }
    }

    /**
     * 出最小的单张
     */
    private List<String> playSmallestSingle(PokerHand hand) {
        if (hand.isEmpty()) {
            return Collections.emptyList();
        }

        // 按斗地主排序值排序，取最小的
        List<Poker> sortedList = hand.getAll().stream()
                .sorted(Comparator.comparingInt(Poker::getLandlordsSortValue))
                .collect(Collectors.toList());
        Poker smallest = sortedList.get(0);

        return Collections.singletonList(smallest.getId());
    }

    /**
     * 尝试压过上家的牌
     * 能压则压最小的，压不了则跳过
     */
    private List<String> playToBeat(PokerHand hand, PatternResult lastPattern) {
        if (hand.isEmpty()) {
            return Collections.emptyList();
        }

        PokerPatternEnum patternType = lastPattern.getPattern();
        List<Poker> handList = hand.getAll();
        int lastValue = lastPattern.getMainValue();

        switch (patternType) {
            case SINGLE:
                return playSingleToBeat(handList, lastValue);

            case PAIR:
                return playPairToBeat(handList, lastValue);

            case PURE_PAIR:
                return playPairToBeat(handList, lastValue);

            case TRIPLE:
                return playTripleToBeat(handList, lastValue);

            case TRIPLE_SINGLE:
                return playTripleWithSingleToBeat(hand, lastValue);

            case TRIPLE_PAIR:
                return playTripleWithPairToBeat(hand, lastValue);

            case STRAIGHT:
                return playStraightToBeat(handList, lastValue);

            case DOUBLE_STRAIGHT:
                return playStraightPairToBeat(handList, lastValue);

            case PLANE:
            case PLANE_SINGLE:
            case PLANE_PAIR:
                return playPlaneToBeat(handList, lastValue, lastPattern.getCount());

            case BOMB:
                return playBombToBeat(handList, lastValue);

            case JOKER_BOMB:
                return Collections.emptyList();

            default:
                return playSmallestSingle(new PokerHand(handList));
        }
    }

    /**
     * 压单张
     */
    private List<String> playSingleToBeat(List<Poker> hand, int lastValue) {
        for (Poker poker : hand) {
            if (poker.getLandlordsSortValue() > lastValue) {
                return Collections.singletonList(poker.getId());
            }
        }
        // 压不了，尝试用炸弹
        return playBombToBeat(hand, lastValue);
    }

    /**
     * 压对子
     */
    private List<String> playPairToBeat(List<Poker> hand, int lastValue) {
        List<Poker> pairs = findPairs(hand);
        for (Poker pair : pairs) {
            if (pair.getLandlordsSortValue() > lastValue) {
                // 找到对应的另一张牌
                String pairId = findPairId(hand, pair);
                if (pairId != null) {
                    return Arrays.asList(pair.getId(), pairId);
                }
            }
        }
        return playBombToBeat(hand, lastValue);
    }

    /**
     * 压三张
     */
    private List<String> playTripleToBeat(List<Poker> hand, int lastValue) {
        List<Poker> triples = findTriples(hand);
        for (Poker triple : triples) {
            if (triple.getLandlordsSortValue() > lastValue) {
                // 找到另外两张相同的牌
                List<String> result = new ArrayList<>();
                result.add(triple.getId());
                for (Poker p : hand) {
                    if (p != triple && p.getValue() == triple.getValue()) {
                        result.add(p.getId());
                        break;
                    }
                }
                return result;
            }
        }
        return playBombToBeat(hand, lastValue);
    }

    /**
     * 压三带单
     */
    private List<String> playTripleWithSingleToBeat(PokerHand hand, int lastValue) {
        List<Poker> triples = findTriples(hand.getAll());
        if (triples.isEmpty()) {
            return playBombToBeat(hand.getAll(), lastValue);
        }

        // 找到最小的能压的三带一
        for (Poker triple : triples) {
            if (triple.getLandlordsSortValue() > lastValue) {
                // 找到了三张，添加一张最小的单牌
                List<String> result = new ArrayList<>();
                result.add(triple.getId());
                // 添加另外两张相同的牌
                int count = 1;
                for (Poker p : hand.getAll()) {
                    if (p != triple && p.getValue() == triple.getValue() && count < 2) {
                        result.add(p.getId());
                        count++;
                    }
                }
                // 添加一张最小的单牌（不是三张中的）
                for (Poker p : hand.getAll()) {
                    if (p.getValue() != triple.getValue()) {
                        result.add(p.getId());
                        break;
                    }
                }
                return result;
            }
        }
        return playBombToBeat(hand.getAll(), lastValue);
    }

    /**
     * 压三带对
     */
    private List<String> playTripleWithPairToBeat(PokerHand hand, int lastValue) {
        List<Poker> triples = findTriples(hand.getAll());
        List<List<Poker>> pairs = findAllPairs(hand.getAll());
        if (triples.isEmpty() || pairs.isEmpty()) {
            return playBombToBeat(hand.getAll(), lastValue);
        }

        for (Poker triple : triples) {
            if (triple.getValue().getValue() > lastValue) {
                // 找到了三张，找一张最小的对子（不是三张中的牌）
                List<Poker> smallestPair = null;
                Set<PokerValueEnum> tripleValues = new HashSet<>();
                tripleValues.add(triple.getValue());
                for (Poker p : hand.getAll()) {
                    if (p.getValue() == triple.getValue()) {
                        tripleValues.add(p.getValue());
                    }
                }

                for (List<Poker> pair : pairs) {
                    if (!tripleValues.contains(pair.get(0).getValue())) {
                        smallestPair = pair;
                        break;
                    }
                }

                if (smallestPair == null) {
                    return playBombToBeat(hand.getAll(), lastValue);
                }

                List<String> result = new ArrayList<>();
                result.add(triple.getId());
                // 添加另外两张相同的牌
                int count = 1;
                for (Poker p : hand.getAll()) {
                    if (p != triple && p.getValue() == triple.getValue() && count < 2) {
                        result.add(p.getId());
                        count++;
                    }
                }
                result.addAll(smallestPair.stream().map(Poker::getId).collect(Collectors.toList()));
                return result;
            }
        }
        return playBombToBeat(hand.getAll(), lastValue);
    }

    /**
     * 压顺子
     */
    private List<String> playStraightToBeat(List<Poker> hand, int lastValue) {
        // 查找所有可能的顺子
        List<List<Poker>> straights = findStraights(hand);
        for (List<Poker> straight : straights) {
            int straightMax = straight.stream()
                    .mapToInt(p -> p.getLandlordsSortValue())
                    .max().orElse(0);
            if (straightMax > lastValue) {
                return straight.stream().map(Poker::getId).collect(Collectors.toList());
            }
        }
        return playBombToBeat(hand, lastValue);
    }

    /**
     * 压连对
     */
    private List<String> playStraightPairToBeat(List<Poker> hand, int lastValue) {
        // 查找所有可能的连对
        List<List<Poker>> doubleStraights = findDoubleStraights(hand);
        for (List<Poker> doubleStraight : doubleStraights) {
            int dsMax = doubleStraight.stream()
                    .mapToInt(p -> p.getLandlordsSortValue())
                    .max().orElse(0);
            if (dsMax > lastValue) {
                return doubleStraight.stream().map(Poker::getId).collect(Collectors.toList());
            }
        }
        return playBombToBeat(hand, lastValue);
    }

    /**
     * 压飞机/飞机带单/飞机带对
     */
    private List<String> playPlaneToBeat(List<Poker> hand, int lastValue, int cardCount) {
        // 查找所有可能的飞机
        List<List<Poker>> planes = findPlanes(hand);
        for (List<Poker> plane : planes) {
            int planeMax = plane.stream()
                    .mapToInt(p -> p.getLandlordsSortValue())
                    .max().orElse(0);
            int planeCardCount = plane.size();

            // 计算带牌数量
            int extraCardsNeeded = cardCount - planeCardCount;
            if (extraCardsNeeded <= 0) {
                // 纯飞机
                if (planeMax > lastValue) {
                    return plane.stream().map(Poker::getId).collect(Collectors.toList());
                }
            } else {
                // 飞机带单或带对
                List<Poker> extras = findExtraCards(hand, plane, extraCardsNeeded);
                if (!extras.isEmpty()) {
                    List<String> result = new ArrayList<>(plane.stream().map(Poker::getId).collect(Collectors.toList()));
                    result.addAll(extras.stream().map(Poker::getId).collect(Collectors.toList()));
                    if (planeMax > lastValue) {
                    return result;
                    }
                }
            }
        }
        return playBombToBeat(hand, lastValue);
    }

    /**
     * 查找所有可能的顺子
     */
    private List<List<Poker>> findStraights(List<Poker> hand) {
        List<List<Poker>> straights = new ArrayList<>();

        // 按面值分组
        Map<Integer, List<Poker>> grouped = hand.stream()
                .collect(Collectors.groupingBy(Poker::getLandlordsSortValue));

        // 获取所有有效面值（3-14，即A）
        Set<Integer> validValues = grouped.keySet().stream()
                .filter(v -> v >= 3 && v <= 14)
                .collect(Collectors.toSet());

        if (validValues.size() < 5) {
            return straights;
        }

        // 按降序排序
        List<Integer> sortedValues = validValues.stream()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        // 查找所有连续段
        List<List<Integer>> runs = findConsecutiveRuns(sortedValues);

        for (List<Integer> run : runs) {
            if (run.size() >= 5) {
                // 生成顺子
                for (int i = 0; i <= run.size() - 5; i++) {
                    List<Poker> straight = new ArrayList<>();
                    for (int j = i; j < i + 5; j++) {
                        straight.addAll(grouped.get(run.get(j)));
                    }
                    if (straight.size() >= 5) {
                        straights.add(straight.stream().limit(5).collect(Collectors.toList()));
                    }
                }
                // 最长的顺子
                List<Poker> longestStraight = new ArrayList<>();
                for (Integer v : run) {
                    for (Poker p : grouped.get(v)) {
                        if (longestStraight.size() < run.size()) {
                            longestStraight.add(p);
                        }
                        if (longestStraight.size() == run.size()) {
                            break;
                        }
                    }
                }
                if (!longestStraight.isEmpty()) {
                    straights.add(longestStraight);
                }
            }
        }

        // 按最大值排序
        straights.sort((a, b) -> {
            int maxA = a.stream().mapToInt(p -> p.getLandlordsSortValue()).max().orElse(0);
            int maxB = b.stream().mapToInt(p -> p.getLandlordsSortValue()).max().orElse(0);
            return Integer.compare(maxA, maxB);
        });

        return straights;
    }

    /**
     * 查找所有连续段
     */
    private List<List<Integer>> findConsecutiveRuns(List<Integer> sortedValues) {
        List<List<Integer>> runs = new ArrayList<>();
        if (sortedValues.isEmpty()) {
            return runs;
        }

        List<Integer> current = new ArrayList<>();
        current.add(sortedValues.get(0));

        for (int i = 1; i < sortedValues.size(); i++) {
            if (sortedValues.get(i - 1) - sortedValues.get(i) == 1) {
                current.add(sortedValues.get(i));
            } else {
                if (current.size() >= 2) {
                    runs.add(new ArrayList<>(current));
                }
                current = new ArrayList<>();
                current.add(sortedValues.get(i));
            }
        }
        if (current.size() >= 2) {
            runs.add(current);
        }

        return runs;
    }

    /**
     * 查找所有可能的连对
     */
    private List<List<Poker>> findDoubleStraights(List<Poker> hand) {
        List<List<Poker>> doubleStraights = new ArrayList<>();

        // 按面值分组并找出有对子的
        Map<Integer, List<Poker>> grouped = hand.stream()
                .collect(Collectors.groupingBy(Poker::getLandlordsSortValue));

        List<Integer> pairValues = new ArrayList<>();
        for (Map.Entry<Integer, List<Poker>> entry : grouped.entrySet()) {
            if (entry.getValue().size() >= 2 && entry.getKey() >= 3 && entry.getKey() <= 14) {
                pairValues.add(entry.getKey());
            }
        }

        if (pairValues.size() < 3) {
            return doubleStraights;
        }

        // 按降序排序
        pairValues.sort(Collections.reverseOrder());

        // 查找连续段
        List<List<Integer>> runs = findConsecutiveRuns(pairValues);

        for (List<Integer> run : runs) {
            if (run.size() >= 3) {
                List<Poker> ds = new ArrayList<>();
                for (Integer v : run) {
                    List<Poker> pair = grouped.get(v);
                    ds.add(pair.get(0));
                    ds.add(pair.get(1));
                }
                doubleStraights.add(ds);
            }
        }

        // 按最大值排序
        doubleStraights.sort((a, b) -> {
            int maxA = a.stream().mapToInt(p -> p.getLandlordsSortValue()).max().orElse(0);
            int maxB = b.stream().mapToInt(p -> p.getLandlordsSortValue()).max().orElse(0);
            return Integer.compare(maxA, maxB);
        });

        return doubleStraights;
    }

    /**
     * 查找所有可能的飞机（纯三张）
     */
    private List<List<Poker>> findPlanes(List<Poker> hand) {
        List<List<Poker>> planes = new ArrayList<>();

        Map<Integer, List<Poker>> grouped = hand.stream()
                .collect(Collectors.groupingBy(Poker::getLandlordsSortValue));

        List<Integer> tripleValues = new ArrayList<>();
        for (Map.Entry<Integer, List<Poker>> entry : grouped.entrySet()) {
            if (entry.getValue().size() >= 3 && entry.getKey() >= 3 && entry.getKey() <= 14) {
                tripleValues.add(entry.getKey());
            }
        }

        if (tripleValues.size() < 2) {
            return planes;
        }

        // 按降序排序
        tripleValues.sort(Collections.reverseOrder());

        // 查找连续段
        List<List<Integer>> runs = findConsecutiveRuns(tripleValues);

        for (List<Integer> run : runs) {
            if (run.size() >= 2) {
                List<Poker> plane = new ArrayList<>();
                for (Integer v : run) {
                    plane.addAll(grouped.get(v));
                }
                planes.add(plane);
            }
        }

        return planes;
    }

    /**
     * 查找带牌
     */
    private List<Poker> findExtraCards(List<Poker> hand, List<Poker> plane, int count) {
        Set<Integer> planeValues = plane.stream()
                .map(Poker::getLandlordsSortValue)
                .collect(Collectors.toSet());

        return hand.stream()
                .filter(p -> !planeValues.contains(p.getLandlordsSortValue()))
                .sorted(Comparator.comparingInt(p -> p.getLandlordsSortValue()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * 压炸弹
     */
    private List<String> playBombToBeat(List<Poker> hand, int lastValue) {
        // 先看能不能用炸弹压
        List<List<Poker>> bombs = findBombs(hand);
        for (List<Poker> bomb : bombs) {
            if (bomb.get(0).getValue().getValue() > lastValue) {
                return bomb.stream().map(Poker::getId).collect(Collectors.toList());
            }
        }

        // 如果是火箭，可以压任何牌
        if (hasRocket(hand)) {
            return getRocketIds(hand);
        }

        // 压不了
        return Collections.emptyList();
    }

    /**
     * 查找对子（返回较大的那张牌，用于比较）
     */
    private List<Poker> findPairs(List<Poker> hand) {
        List<Poker> pairs = new ArrayList<>();
        Set<PokerValueEnum> usedValues = new HashSet<>();

        for (int i = 0; i < hand.size(); i++) {
            Poker current = hand.get(i);
            if (usedValues.contains(current.getValue())) {
                continue;
            }
            for (int j = i + 1; j < hand.size(); j++) {
                if (current.getValue() == hand.get(j).getValue()) {
                    pairs.add(hand.get(j));
                    usedValues.add(current.getValue());
                    break;
                }
            }
        }
        pairs.sort(Comparator.comparingInt(p -> p.getValue().getValue()));
        return pairs;
    }

    /**
     * 查找所有对子（返回配对完整的对子列表）
     */
    private List<List<Poker>> findAllPairs(List<Poker> hand) {
        List<List<Poker>> pairs = new ArrayList<>();
        List<Poker> used = new ArrayList<>();

        for (int i = 0; i < hand.size(); i++) {
            if (used.contains(hand.get(i))) continue;

            for (int j = i + 1; j < hand.size(); j++) {
                if (used.contains(hand.get(j))) continue;

                if (hand.get(i).getValue() == hand.get(j).getValue()) {
                    List<Poker> pair = Arrays.asList(hand.get(i), hand.get(j));
                    pairs.add(pair);
                    used.add(hand.get(i));
                    used.add(hand.get(j));
                    break;
                }
            }
        }

        // 按对子的面值排序
        pairs.sort(Comparator.comparingInt(p -> p.get(0).getValue().getValue()));
        return pairs;
    }

    /**
     * 查找三张
     */
    private List<Poker> findTriples(List<Poker> hand) {
        List<Poker> triples = new ArrayList<>();
        Set<PokerValueEnum> usedValues = new HashSet<>();

        for (int i = 0; i < hand.size(); i++) {
            Poker current = hand.get(i);
            if (usedValues.contains(current.getValue())) {
                continue;
            }
            int count = 1;
            for (int j = i + 1; j < hand.size(); j++) {
                if (current.getValue() == hand.get(j).getValue()) {
                    count++;
                }
            }
            if (count >= 3) {
                triples.add(current);
                usedValues.add(current.getValue());
            }
        }
        triples.sort(Comparator.comparingInt(p -> p.getValue().getValue()));
        return triples;
    }

    /**
     * 查找炸弹
     */
    private List<List<Poker>> findBombs(List<Poker> hand) {
        List<List<Poker>> bombs = new ArrayList<>();
        Map<PokerValueEnum, List<Poker>> grouped = new HashMap<>();

        for (Poker p : hand) {
            grouped.computeIfAbsent(p.getValue(), k -> new ArrayList<>()).add(p);
        }

        for (List<Poker> group : grouped.values()) {
            if (group.size() >= 4) {
                bombs.add(group);
            }
        }

        // 按面值排序
        bombs.sort(Comparator.comparingInt(b -> b.get(0).getValue().getValue()));
        return bombs;
    }

    /**
     * 查找对子的另一张牌ID
     */
    private String findPairId(List<Poker> hand, Poker pair) {
        for (Poker p : hand) {
            if (!p.equals(pair) && p.getValue() == pair.getValue()) {
                return p.getId();
            }
        }
        return null;
    }

    /**
     * 是否有火箭
     */
    private boolean hasRocket(List<Poker> hand) {
        boolean hasSmallJoker = false;
        boolean hasBigJoker = false;

        for (Poker p : hand) {
            int value = p.getValue().getValue();
            if (value == 16) {
                hasSmallJoker = true;
            } else if (value == 17) {
                hasBigJoker = true;
            }
        }

        return hasSmallJoker && hasBigJoker;
    }

    /**
     * 获取火箭的牌ID
     */
    private List<String> getRocketIds(List<Poker> hand) {
        List<String> rocket = new ArrayList<>();
        for (Poker p : hand) {
            int value = p.getValue().getValue();
            if (value == 16 || value == 17) {
                rocket.add(p.getId());
            }
        }
        return rocket;
    }
}
