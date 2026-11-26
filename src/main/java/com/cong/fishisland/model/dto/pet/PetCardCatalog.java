package com.cong.fishisland.model.dto.pet;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PetCardCatalog {
    private PetCardCatalog() {}

    @Data
    public static class Card {
        private String id;
        private String name;
        private String grade;
        private JsonNode stats;
        private JsonNode battle;
        private String artKey;
        private Integer artTile;
        private boolean artPortrait;
        private int sortOrder;
        private Integer version;
        private List<String> poolIds;
    }

    @Data
    public static class Pool {
        private String id;
        private String name;
        private Map<String, Integer> weights;
        private String coverId;
        private Integer unitPrice;
        private int sortOrder;
        private Integer version;
    }

    @Value
    public static class Atlas {
        String id;
        String name;
        boolean portrait;
    }

    @Value
    public static class Catalog {
        List<Pool> pools;
        List<Card> cards;
        List<Bond> factionBonds;
    }

    @Value
    public static class Bond {
        String id;
        String name;
        Set<String> cardIds;
        List<BondBonus> three;
        List<BondBonus> six;
    }

    @Value
    public static class BondBonus {
        String attribute;
        int percentagePoints;
    }

    @Value
    public static class CardPage {
        List<Card> items;
        long total;
    }
}
