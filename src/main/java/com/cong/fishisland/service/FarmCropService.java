package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.farm.FarmCrop;

import java.util.List;

public interface FarmCropService {
    
    List<FarmCrop> getAllCrops();
    
    List<FarmCrop> getCropsByCategory(String category);
    
    FarmCrop getCropById(Long cropId);
    
    FarmCrop createCrop(FarmCrop crop);
    
    List<String> getCategories();
}