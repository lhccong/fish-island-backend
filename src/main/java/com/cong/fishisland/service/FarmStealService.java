package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FarmStealService {
    
    @Transactional
    FarmStealRecord steal(Long stealerId, Long plantRecordId);
    
    boolean checkCooldown(Long stealerId, Long plantRecordId);
    
    boolean validateFriend(Long stealerId, Long ownerId);
    
    void updateTaskProgress(Long stealerId);
    
    List<FarmStealRecord> getStealRecordsByStealer(Long stealerId);
    
    List<FarmStealRecordVO> getStealRecordsByOwner(Long ownerId);
}