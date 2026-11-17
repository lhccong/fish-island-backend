package com.cong.fishisland.model.vo.pet;

import lombok.Data;

@Data
public class PetTreasureVO {
    private long serverNow;
    private long resetAt;
    private long nextAvailableAt;
    private int used;
    private Active active;
    private Reward lastReward;

    @Data
    public static class Active {
        private String id;
        private String siteId;
        private long readyAt;
    }

    @Data
    public static class Reward {
        private String id;
        private String siteId;
        private int points;
    }
}
