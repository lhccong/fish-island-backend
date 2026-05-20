package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.farm.FarmLand;

import java.util.List;

public interface FarmLandService {

    List<FarmLand> getLandsByUserId(Long userId);

    FarmLand getLand(Long landId);

    void initLands(Long userId);

    FarmLand plant(Long userId, Long landId, Long cropId);

    FarmLand harvest(Long userId, Long landId);

    void updateLandStatus();
}