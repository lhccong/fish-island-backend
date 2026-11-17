package com.cong.fishisland.model.entity.pet;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PetTreasureDig {
    private String id;
    private Long userId;
    private LocalDate digDay;
    private Integer slot;
    private String siteId;
    private Long readyAt;
    private Integer reward;
    private Long completedAt;
}
