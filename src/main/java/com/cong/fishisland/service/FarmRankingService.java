package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.RankingDTO;
import com.cong.fishisland.model.entity.farm.FarmRanking;

import java.util.List;

public interface FarmRankingService {
    
    List<RankingDTO> getTodayStealExpRanking();

    List<RankingDTO> getTodayStealCountRanking();
    
    List<RankingDTO> getTodayDefenseRanking();
    
    List<RankingDTO> getTotalStealExpRanking();
    
    List<RankingDTO> getTotalStealCountRanking();

    List<RankingDTO> getTotalDefenseRanking();
    
    void updateStealRanking(Long stealerId, int expGained);
    
    void updateDefenseRanking(Long ownerId, int damage);
}