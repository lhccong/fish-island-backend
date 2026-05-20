package com.cong.fishisland.model.dto.farm.request;

import lombok.Data;

@Data
public class HarvestRequest {
    private Long userId;

    private Long landId;
}