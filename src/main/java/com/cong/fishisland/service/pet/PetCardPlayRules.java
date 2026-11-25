package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.cong.fishisland.model.dto.pet.PetCardPlay.LevelProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import static com.cong.fishisland.service.pet.PetCardCatalogRules.require;

public final class PetCardPlayRules {
    static final String ISLAND_POOL = "island";
    static final int DAILY_FREE = 2;
    static final int PACK_SIZE = 5;
    static final int SHOWCASE_LIMIT = 6;
    public static final int MAX_LEVEL = 60;
    private static final int FIRST_LEVEL_OPENINGS = 3;
    private static final double LEVEL_GROWTH = 1.07;
    private static final int[] OPENINGS_AT_LEVEL = openingsAtLevels();

    private PetCardPlayRules() {}

    private static int[] openingsAtLevels() {
        int[] thresholds = new int[MAX_LEVEL + 1];
        for (int level = 1; level < MAX_LEVEL; level++) {
            int cost = (int) Math.ceil(FIRST_LEVEL_OPENINGS * Math.pow(LEVEL_GROWTH, level - 1));
            thresholds[level + 1] = thresholds[level] + cost;
        }
        return thresholds;
    }

    public static int level(long opened) {
        int level = 1;
        while (level < MAX_LEVEL && opened >= OPENINGS_AT_LEVEL[level + 1]) level++;
        return level;
    }

    static int openingsToNextLevel(long opened, int level) {
        return level == MAX_LEVEL ? 0 : OPENINGS_AT_LEVEL[level + 1] - (int) opened;
    }

    static LevelProgress levelProgress(long opened) {
        int level = level(opened);
        int start = OPENINGS_AT_LEVEL[level];
        return level == MAX_LEVEL ? new LevelProgress(level, 0, 0)
                : new LevelProgress(level, (int) opened - start, OPENINGS_AT_LEVEL[level + 1] - start);
    }

    static int requiredLevel(String poolId) {
        return "kingdoms".equals(poolId) ? 10 : 1;
    }

    static int purchaseCost(int quantity, int unitPrice) {
        require(quantity == 1 || quantity == 10, "仅支持一次购买 1 包或 10 包");
        return unitPrice * (quantity == 10 ? 9 : 1);
    }

    static Integer upgradeCost(int stars) {
        return stars < 5 ? 1 << (stars - 1) : null;
    }

    static int statAtStars(int base, int stars) {
        return base > 0 ? base + (stars - 1) * Math.max(1, base / 5) : base;
    }

    static List<Card> draw(Pool pool, List<Card> cards, boolean first, IntUnaryOperator random) {
        Map<String, List<Card>> byGrade = cards.stream().collect(Collectors.groupingBy(Card::getGrade));
        List<Map.Entry<String, Integer>> grades = pool.getWeights().entrySet().stream()
                .filter(entry -> entry.getValue() > 0 && byGrade.containsKey(entry.getKey()))
                .collect(Collectors.toList());
        int total = grades.stream().mapToInt(Map.Entry::getValue).sum();
        require(total > 0, "这个卡池暂未开放");
        List<Card> guaranteed = first && ISLAND_POOL.equals(pool.getId()) ? byGrade.get("shiny") : null;
        List<Card> result = new ArrayList<>(PACK_SIZE);
        for (int position = 0; position < PACK_SIZE; position++) {
            if (position == PACK_SIZE - 1 && guaranteed != null) {
                result.add(guaranteed.get(random.applyAsInt(guaranteed.size())));
                continue;
            }
            int roll = random.applyAsInt(total);
            for (Map.Entry<String, Integer> grade : grades) {
                roll -= grade.getValue();
                if (roll < 0) {
                    List<Card> available = byGrade.get(grade.getKey());
                    result.add(available.get(random.applyAsInt(available.size())));
                    break;
                }
            }
        }
        return result;
    }
}
