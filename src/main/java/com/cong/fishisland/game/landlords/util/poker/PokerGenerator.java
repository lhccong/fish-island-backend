package com.cong.fishisland.game.landlords.util.poker;

import com.cong.fishisland.game.landlords.enums.poker.PokerTypeEnum;
import com.cong.fishisland.game.landlords.enums.poker.PokerValueEnum;
import com.cong.fishisland.game.landlords.model.poker.Poker;
import com.cong.fishisland.game.landlords.model.poker.PokerHand;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 扑克牌生成器
 *
 * @author cong
 */
public class PokerGenerator {

    private PokerGenerator() {
    }

    /**
     * 生成一副完整的扑克牌
     */
    public static PokerHand generateFullDeck() {
        List<Poker> pokers = new ArrayList<>();

        for (PokerTypeEnum type : PokerTypeEnum.values()) {
            for (PokerValueEnum value : PokerValueEnum.values()) {
                if (!value.isJoker()) {
                    pokers.add(new Poker(type, value));
                }
            }
        }

        pokers.add(Poker.createJoker(false));
        pokers.add(Poker.createJoker(true));

        return new PokerHand(pokers);
    }

    /**
     * 生成指定数量的扑克牌 (用于多副牌)
     */
    public static PokerHand generateDeck(int deckCount) {
        List<Poker> pokers = new ArrayList<>();
        for (int deck = 0; deck < deckCount; deck++) {
            for (Poker p : generateFullDeck().getAll()) {
                Poker copy = new Poker(p.getType(), p.getValue());
                copy.setUniversal(p.isUniversal());
                copy.setUniversalTarget(p.getUniversalTarget());
                copy.setDeckIndex(deck);
                pokers.add(copy);
            }
        }
        return new PokerHand(pokers);
    }

    /**
     * 洗牌
     */
    public static PokerHand shuffle(PokerHand deck) {
        List<Poker> pokers = new ArrayList<>(deck.getAll());
        Collections.shuffle(pokers);
        return new PokerHand(pokers);
    }

    /**
     * 发牌给指定数量的玩家
     */
    public static List<PokerHand> deal(PokerHand deck, int playerCount, int cardsPerPlayer) {
        List<Poker> pokers = new ArrayList<>(deck.getAll());
        int totalCards = Math.min(cardsPerPlayer * playerCount, pokers.size());

        List<PokerHand> hands = IntStream.range(0, playerCount)
                .mapToObj(i -> IntStream.range(0, cardsPerPlayer)
                        .mapToObj(j -> {
                            int index = i * cardsPerPlayer + j;
                            return index < totalCards ? pokers.get(index) : null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .map(PokerHand::new)
                .collect(Collectors.toList());

        return hands;
    }

    /**
     * 发牌给指定数量的玩家，并保留底牌
     */
    public static DealResult dealWithBottom(PokerHand deck, int playerCount, int cardsPerPlayer, int bottomCards) {
        List<PokerHand> hands = deal(deck, playerCount, cardsPerPlayer);
        List<Poker> allPokers = new ArrayList<>(deck.getAll());
        int cardsDealt = playerCount * cardsPerPlayer;

        List<Poker> bottomList = IntStream.range(0, bottomCards)
                .filter(i -> allPokers.size() > cardsDealt + i)
                .mapToObj(allPokers::get)
                .collect(Collectors.toList());

        return new DealResult(hands, new PokerHand(bottomList));
    }

    /**
     * 发牌结果
     */
    public static class DealResult {
        private final List<PokerHand> hands;
        private final PokerHand bottom;

        public DealResult(List<PokerHand> hands, PokerHand bottom) {
            this.hands = hands;
            this.bottom = bottom;
        }

        public List<PokerHand> getHands() {
            return hands;
        }

        public PokerHand getBottom() {
            return bottom;
        }
    }

    /**
     * 随机选择一张牌作为癞子
     */
    public static Poker randomUniversal(PokerHand deck) {
        List<Poker> nonJokers = new ArrayList<>();
        for (Poker p : deck.getAll()) {
            if (!p.getValue().isJoker()) {
                nonJokers.add(p);
            }
        }
        return nonJokers.isEmpty() ? null : nonJokers.get(new Random().nextInt(nonJokers.size()));
    }

    /**
     * 随机选择一张非癞子的牌作为癞子
     */
    public static Poker randomUniversalExcludeJoker(PokerHand deck) {
        List<Poker> validCards = new ArrayList<>();
        for (Poker p : deck.getAll()) {
            if (!p.getValue().isJoker() && !p.isUniversal()) {
                validCards.add(p);
            }
        }
        return validCards.isEmpty() ? null : validCards.get(new Random().nextInt(validCards.size()));
    }

    /**
     * 从字符串解析扑克牌
     */
    public static PokerHand parseFromString(String input) {
        if (input == null || input.isEmpty()) {
            return new PokerHand();
        }

        PokerHand hand = new PokerHand();
        input = input.replace("0", "10");
        for (char c : input.toLowerCase().toCharArray()) {
            Poker poker = parseSingle(String.valueOf(c));
            if (poker != null) {
                hand.add(poker);
            }
        }

        return hand;
    }

    /**
     * 解析单张牌
     */
    public static Poker parseSingle(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        input = input.toLowerCase().trim();

        if ("sx".equals(input) || "小王".equals(input)) {
            return Poker.createJoker(false);
        }
        if ("dx".equals(input) || "大王".equals(input) || "王炸".equals(input)) {
            return Poker.createJoker(true);
        }

        PokerValueEnum value = PokerValueEnum.getByAlias(input);
        if (value != null) {
            if (value.isJoker()) {
                return Poker.createJoker(value == PokerValueEnum.BIG_JOKER);
            }
            return new Poker(PokerTypeEnum.SPADE, value);
        }

        return null;
    }

    /**
     * 解析牌 ID
     */
    public static Poker parseById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        id = id.trim();

        if (id.startsWith("*")) {
            String target = id.substring(1);
            PokerValueEnum value = PokerValueEnum.getByAlias(target);
            if (value != null && !value.isJoker()) {
                Poker poker = new Poker(null, value);
                poker.setUniversal(true);
                return poker;
            }
        }

        if (id.length() >= 2) {
            String first = id.substring(0, 1);
            String second = id.substring(1);

            PokerTypeEnum type = null;
            for (PokerTypeEnum t : PokerTypeEnum.values()) {
                if (t.getSymbol().equals(first) || t.getName().equalsIgnoreCase(first)) {
                    type = t;
                    break;
                }
            }

            PokerValueEnum value = PokerValueEnum.getByAlias(second);
            if (type != null && value != null && !value.isJoker()) {
                return new Poker(type, value);
            }
        }

        return parseSingle(id);
    }
}
