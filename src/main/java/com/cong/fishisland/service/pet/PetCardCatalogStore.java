package com.cong.fishisland.service.pet;

import com.cong.fishisland.mapper.PetCardCatalogMapper;
import com.cong.fishisland.mapper.PetCardCatalogMapper.Membership;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Atlas;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Card;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.CardPage;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.Pool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PetCardCatalogStore {
    private final PetCardCatalogMapper mapper;
    private final ObjectMapper json;

    List<Pool> pools(boolean lock) {
        return mapper.pools(lock);
    }

    Pool pool(String id) {
        return mapper.pool(id);
    }

    List<Card> poolCards(String id) {
        return mapper.poolCards(id);
    }

    List<Card> cards() {
        return loadMemberships(mapper.cards());
    }

    CardPage cards(String poolId, String query, int page) {
        long total = mapper.countCards(poolId, query);
        List<Card> cards = mapper.cardPage(poolId, query, (page - 1L) * 20);
        return new CardPage(loadMemberships(cards), total);
    }

    private List<Card> loadMemberships(List<Card> cards) {
        if (cards.isEmpty()) return cards;
        List<String> ids = cards.stream().map(Card::getId).collect(Collectors.toList());
        Map<String, List<String>> groups = new HashMap<>();
        for (Membership membership : mapper.cardMemberships(ids)) {
            groups.computeIfAbsent(membership.getCardId(), id -> new ArrayList<>()).add(membership.getPoolId());
        }
        cards.forEach(card -> card.setPoolIds(groups.getOrDefault(card.getId(), Collections.emptyList())));
        return cards;
    }

    Integer cardVersion(String id) {
        return mapper.lockCardVersion(id);
    }

    List<String> memberships(String id) {
        return mapper.memberships(id);
    }

    boolean containsCard(String pool, String card) {
        return mapper.containsCard(pool, card);
    }

    boolean hasShinyCard(String pool) {
        return mapper.hasShinyCard(pool);
    }

    boolean drawable(Pool pool) {
        return mapper.grades(pool.getId()).stream().anyMatch(grade -> pool.getWeights().getOrDefault(grade, 0) > 0);
    }

    void saveCard(Card card) {
        String stats = card.getStats().toString();
        String battle = card.getBattle().toString();
        if (card.getVersion() == null) {
            mapper.insertCard(card, stats, battle);
        } else {
            mapper.updateCard(card, stats, battle);
        }
    }

    void setMemberships(Card card, List<String> before) {
        for (String id : before) {
            if (!card.getPoolIds().contains(id)) {
                mapper.deleteMembership(id, card.getId());
                mapper.clearCover(id, card.getId());
            }
        }
        for (String id : card.getPoolIds()) {
            if (!before.contains(id)) {
                mapper.insertMembership(id, card.getId());
            }
        }
    }

    void savePool(Pool pool) {
        String weights = json.valueToTree(pool.getWeights()).toString();
        if (pool.getVersion() == null) {
            int price = pool.getUnitPrice() == null ? ("island".equals(pool.getId()) ? 50 : 100) : pool.getUnitPrice();
            mapper.insertPool(pool, weights, price);
        } else {
            mapper.updatePool(pool, weights);
        }
    }

    List<Atlas> atlases() {
        return mapper.atlases();
    }

    boolean hasAtlas(String id) {
        return mapper.hasAtlas(id);
    }

    Atlas registerAtlas(Atlas atlas) {
        mapper.insertAtlas(atlas);
        return mapper.atlas(atlas.getId());
    }

    boolean startSeed(String id) {
        return mapper.insertSeed(id) == 1;
    }
}
