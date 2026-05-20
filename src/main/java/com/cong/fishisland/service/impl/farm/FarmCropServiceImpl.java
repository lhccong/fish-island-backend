package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.model.dto.farm.CropCategoryVO;
import com.cong.fishisland.model.dto.farm.CropDTO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.enums.farm.FarmCropCategoryEnum;
import com.cong.fishisland.service.FarmCropService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmCropServiceImpl extends ServiceImpl<FarmCropMapper, FarmCrop> implements FarmCropService {

    @Override
    public List<FarmCrop> getAllCrops() {
        return list();
    }

    @Override
    public List<FarmCrop> getCropsByCategory(String category) {
        return list(new LambdaQueryWrapper<FarmCrop>()
                .eq(FarmCrop::getCategory, category));
    }

    @Override
    public FarmCrop getCropById(Long cropId) {
        return getById(cropId);
    }

    @Override
    public FarmCrop createCrop(FarmCrop crop) {
        crop.setCreateTime(LocalDateTime.now());
        save(crop);
        return crop;
    }

    @Override
    public List<CropCategoryVO> getCategories() {
        return FarmCropCategoryEnum.all().stream()
                .map(c -> new CropCategoryVO(c.getValue(), c.getLabel()))
                .collect(Collectors.toList());
    }

    @Override
    public CropDTO toDTO(FarmCrop crop, Integer farmLevel) {
        if (crop == null) {
            return null;
        }
        CropDTO dto = new CropDTO();
        dto.setId(crop.getId());
        dto.setName(crop.getName());
        dto.setCategory(crop.getCategory());
        dto.setGrowthTime(crop.getGrowthTime());
        dto.setExperience(crop.getExperience());
        dto.setCoin(crop.getCoin());
        dto.setRarity(crop.getRarity());
        int unlockLevel = crop.getUnlockLevel() != null ? crop.getUnlockLevel() : 1;
        dto.setUnlockLevel(unlockLevel);
        dto.setLocked(!isUnlocked(crop, farmLevel));
        dto.setIcon(crop.getIcon());
        dto.setDescription(crop.getDescription());
        return dto;
    }

    @Override
    public List<CropDTO> toDTOList(List<FarmCrop> crops, Integer farmLevel) {
        if (crops == null || crops.isEmpty()) {
            return Collections.emptyList();
        }
        return crops.stream().map(c -> toDTO(c, farmLevel)).collect(Collectors.toList());
    }

    @Override
    public boolean isUnlocked(FarmCrop crop, Integer farmLevel) {
        if (crop == null) {
            return false;
        }
        int requiredLevel = crop.getUnlockLevel() != null ? crop.getUnlockLevel() : 1;
        int currentLevel = farmLevel != null ? farmLevel : 1;
        return currentLevel >= requiredLevel;
    }
}
