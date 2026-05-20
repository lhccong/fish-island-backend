package com.cong.fishisland.controller.farm;


import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.CropDTO;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.service.FarmCropService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/crop")
public class CropController {

    @Autowired
    private FarmCropService cropService;

    @GetMapping("/all")
    @ApiOperation(value = "获取所有作物列表")
    public BaseResponse<List<CropDTO>> getAllCrops() {
        List<FarmCrop> crops = cropService.getAllCrops();
        List<CropDTO> dtos = crops.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResultUtils.success(dtos);
    }

    @GetMapping("/category/{category}")
    @ApiOperation(value = "根据分类获取作物列表")
    public BaseResponse<List<CropDTO>> getCropsByCategory(@PathVariable String category) {
        List<FarmCrop> crops = cropService.getCropsByCategory(category);
        List<CropDTO> dtos = crops.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResultUtils.success(dtos);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID获取作物详情")
    public BaseResponse<CropDTO> getCropById(@PathVariable Long id) {
        FarmCrop crop = cropService.getCropById(id);
        if (crop == null) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(convertToDTO(crop));
    }

    @GetMapping("/categories")
    @ApiOperation(value = "获取所有作物分类")
    public BaseResponse<List<String>> getCategories() {
        return ResultUtils.success(cropService.getCategories());
    }

    private CropDTO convertToDTO(FarmCrop crop) {
        CropDTO dto = new CropDTO();
        dto.setId(crop.getId());
        dto.setName(crop.getName());
        dto.setCategory(crop.getCategory());
        dto.setGrowthTime(crop.getGrowthTime());
        dto.setExperience(crop.getExperience());
        dto.setCoin(crop.getCoin());
        dto.setRarity(crop.getRarity());
        dto.setIcon(crop.getIcon());
        dto.setDescription(crop.getDescription());
        return dto;
    }
}