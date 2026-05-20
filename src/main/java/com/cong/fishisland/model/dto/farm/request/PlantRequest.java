package com.cong.fishisland.model.dto.farm.request;

import lombok.Data;

@Data
public class PlantRequest {
    private Long userId;

    private Long landId;

    private Long cropId;
}