package com.cong.fishisland.service.impl.farm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.mapper.farm.FarmCropMapper;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.service.FarmCropService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        crop.setCreatedAt(LocalDateTime.now());
        save(crop);
        return crop;
    }

    @Override
    public List<String> getCategories() {
        return List.of("grain", "vegetable", "fruit", "flower");
    }
}
