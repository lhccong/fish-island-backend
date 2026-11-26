package com.cong.fishisland.service.pet;

import com.cong.fishisland.model.dto.pet.PetCardCatalog.Bond;
import com.cong.fishisland.model.dto.pet.PetCardCatalog.BondBonus;
import com.cong.fishisland.model.vo.pet.PetEquipStatsVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PetCardBonds {
    static final List<Bond> DEFINITIONS = loadDefinitions();

    private PetCardBonds() {}

    static void addTo(Set<String> cardIds, PetEquipStatsVO stats) {
        for (Bond bond : DEFINITIONS) {
            long count = cardIds.stream().filter(bond.getCardIds()::contains).count();
            if (count >= 3) addBonuses(bond.getThree(), stats);
            if (count >= 6) addBonuses(bond.getSix(), stats);
        }
    }

    private static List<Bond> loadDefinitions() {
        Map<String, Set<String>> members = new HashMap<>();
        try (InputStream input = new ClassPathResource("pet-cards/original-catalog.json").getInputStream()) {
            JsonNode seed = new ObjectMapper().readTree(input);
            Map<String, String> factionsByAtlas = new HashMap<>();
            for (JsonNode atlas : seed.path("atlases")) {
                factionsByAtlas.put(atlas.path("file").asText(), atlas.path("faction").asText());
            }
            Set<String> assigned = new HashSet<>();
            for (JsonNode entry : seed.path("cards")) {
                String faction = factionsByAtlas.get(entry.path("atlas").asText());
                String cardId = entry.path("card").path("id").asText();
                if (faction == null || faction.isEmpty() || cardId.isEmpty() || !assigned.add(cardId)) {
                    throw new IllegalStateException("Invalid faction membership for card " + cardId);
                }
                members.computeIfAbsent(faction, key -> new LinkedHashSet<>()).add(cardId);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read card factions", e);
        }
        List<Bond> definitions = Arrays.asList(
                new Bond("island", "鱼岛", takeMembers(members, "island"),
                        bonuses("lifesteal", 4, "dodgeRate", 4),
                        bonuses("lifestealResistance", 5, "dodgeResistance", 5)),
                new Bond("shu", "蜀", takeMembers(members, "shu"),
                        bonuses("comboRate", 5, "critRate", 4),
                        bonuses("comboResistance", 6, "critResistance", 6)),
                new Bond("wu", "吴", takeMembers(members, "wu"),
                        bonuses("dodgeRate", 5, "blockRate", 4),
                        bonuses("dodgeResistance", 6, "blockResistance", 6)),
                new Bond("wei", "魏", takeMembers(members, "wei"),
                        bonuses("lifesteal", 5, "critRate", 4),
                        bonuses("lifestealResistance", 6, "critResistance", 6)),
                new Bond("warlords", "群雄", takeMembers(members, "warlords"),
                        bonuses("critRate", 5, "dodgeRate", 4),
                        bonuses("comboResistance", 6, "blockResistance", 6)));
        if (!members.isEmpty()) throw new IllegalStateException("Unknown card factions: " + members.keySet());
        return Collections.unmodifiableList(definitions);
    }

    private static Set<String> takeMembers(Map<String, Set<String>> members, String id) {
        Set<String> cards = members.remove(id);
        if (cards == null || cards.size() < 6) throw new IllegalStateException("Incomplete card faction: " + id);
        return Collections.unmodifiableSet(cards);
    }

    private static List<BondBonus> bonuses(String first, int firstPoints, String second, int secondPoints) {
        return Collections.unmodifiableList(Arrays.asList(
                new BondBonus(first, firstPoints), new BondBonus(second, secondPoints)));
    }

    private static void addBonuses(List<BondBonus> bonuses, PetEquipStatsVO stats) {
        for (BondBonus bonus : bonuses) {
            double rate = bonus.getPercentagePoints() / 100.0;
            switch (bonus.getAttribute()) {
                case "comboRate": stats.setComboRate(stats.getComboRate() + rate); break;
                case "critRate": stats.setCritRate(stats.getCritRate() + rate); break;
                case "dodgeRate": stats.setDodgeRate(stats.getDodgeRate() + rate); break;
                case "blockRate": stats.setBlockRate(stats.getBlockRate() + rate); break;
                case "lifesteal": stats.setLifesteal(stats.getLifesteal() + rate); break;
                case "comboResistance": stats.setComboResistance(stats.getComboResistance() + rate); break;
                case "critResistance": stats.setCritResistance(stats.getCritResistance() + rate); break;
                case "dodgeResistance": stats.setDodgeResistance(stats.getDodgeResistance() + rate); break;
                case "blockResistance": stats.setBlockResistance(stats.getBlockResistance() + rate); break;
                case "lifestealResistance": stats.setLifestealResistance(stats.getLifestealResistance() + rate); break;
                default: throw new IllegalArgumentException("Unknown card bond attribute: " + bonus.getAttribute());
            }
        }
    }
}
