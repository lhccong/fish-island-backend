package com.cong.fishisland.service;



import com.cong.fishisland.model.entity.farm.FarmCollection;

import java.util.List;

public interface FarmCollectionService {
    
    List<FarmCollection> getUserCollections(Long userId);
    
    void updateCollection(Long userId, Long cropId);
    
    long getObtainedCount(Long userId);
    
    void initCollections(Long userId);
}