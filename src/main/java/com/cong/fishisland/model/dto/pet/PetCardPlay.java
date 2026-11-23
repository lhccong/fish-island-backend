package com.cong.fishisland.model.dto.pet;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.List;

public final class PetCardPlay {
    private PetCardPlay() {}

    @Data
    public static class Command {
        private String requestId;
        private String type;
        private String poolId;
        private Integer quantity;
        private Integer unitPrice;
        private String openingId;
        private String cardId;
        private Integer stars;
    }

    @Value
    public static class Stock {
        String poolId;
        int quantity;
        long opened;
    }

    @Value
    public static class Owned {
        String cardId;
        int stars;
        long duplicates;
        boolean showcased;
        Integer upgradeCost;
    }

    @Data
    public static class Pull {
        private String cardId;
        private String name;
        private boolean duplicate;

        public Pull() {}

        public Pull(String cardId, String name, boolean duplicate) {
            this.cardId = cardId;
            this.name = name;
            this.duplicate = duplicate;
        }
    }

    @Value
    public static class Opening {
        String id;
        String poolId;
        String poolName;
        String openedAt;
        List<Pull> cards;
    }

    @Value
    public static class History {
        List<Opening> items;
        String nextCursor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchResult {
        private int openedPacks;
        private int newCards;
        private int duplicateCards;
        private int upgradedCards;
        private int addedStars;
        private long spentDuplicates;
    }

    @Value
    public static class State {
        String day;
        int freeRemaining;
        long opened;
        long level;
        int xpToNext;
        long balance;
        List<Stock> stock;
        List<Owned> collection;
        Opening pending;
        BatchResult batch;
    }
}
