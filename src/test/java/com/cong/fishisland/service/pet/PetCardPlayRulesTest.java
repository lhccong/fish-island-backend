package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.cong.fishisland.service.pet.PetCardPlayRules.draw;
import static com.cong.fishisland.service.pet.PetCardPlayRules.level;
import static com.cong.fishisland.service.pet.PetCardPlayRules.openingsToNextLevel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetCardPlayRulesTest {
    @Test
    void reservesTheFifthDrawForShinyOnlyOnTheFirstIslandPack() {
        Card common = new Card();
        common.setGrade("common");
        Card shiny = new Card();
        shiny.setGrade("shiny");
        List<Card> cards = Arrays.asList(common, shiny);
        Pool pool = new Pool();
        pool.setId("island");
        pool.setWeights(Collections.singletonMap("common", 1));
        assertEquals(Arrays.asList(common, common, common, common, shiny), draw(pool, cards, true, bound -> 0));
        assertEquals(Collections.nCopies(5, common), draw(pool, cards, false, bound -> 0));
        for (String poolId : Arrays.asList("kingdoms", "expansion")) {
            pool.setId(poolId);
            assertEquals(Collections.nCopies(5, common), draw(pool, cards, true, bound -> 0));
        }
        pool.setWeights(Collections.singletonMap("shiny", 1));
        assertEquals(Collections.nCopies(5, shiny), draw(pool, cards, true, bound -> 0));
    }

    @Test
    void increasesUpgradeCostsAndStopsAtSixty() {
        long opened = 0;
        int previousCost = 0;
        for (int current = 1; current < 60; current++) {
            assertEquals(current, level(opened));
            int cost = openingsToNextLevel(opened, current);
            assertTrue(cost >= previousCost);
            long beforeUpgrade = opened + cost - 1;
            assertEquals(current, level(beforeUpgrade));
            assertEquals(1, openingsToNextLevel(beforeUpgrade, current));
            opened += cost;
            previousCost = cost;
        }
        assertEquals(2307, opened);
        assertEquals(60, level(opened));
        assertEquals(60, level(Long.MAX_VALUE));
        assertEquals(0, openingsToNextLevel(Long.MAX_VALUE, 60));
    }

    @Test
    void startsWithThreeOpeningsAndUnlocksLevelTenAtForty() {
        assertEquals(3, openingsToNextLevel(0, 1));
        assertEquals(2, level(3));
        assertEquals(4, openingsToNextLevel(3, 2));
        assertEquals(9, level(39));
        assertEquals(10, level(40));
        assertEquals(6, openingsToNextLevel(40, 10));
    }
}
