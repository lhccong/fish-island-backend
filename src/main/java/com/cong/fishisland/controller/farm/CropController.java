package com.cong.fishisland.controller.farm;


import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.model.dto.farm.CropCategoryVO;
import com.cong.fishisland.model.dto.farm.CropDTO;
import com.cong.fishisland.model.enums.farm.FarmCropCategoryEnum;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.service.FarmCropService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crop")
public class CropController {

    @Autowired
    private FarmCropService cropService;

    @Autowired
    private FarmUserService farmUserService;

    @GetMapping("/all")
    @ApiOperation(value = "获取所有作物列表")
    public BaseResponse<List<CropDTO>> getAllCrops() {
        int farmLevel = getCurrentFarmLevel();
        return ResultUtils.success(cropService.toDTOList(cropService.getAllCrops(), farmLevel));
    }

    @GetMapping("/category/{category}")
    @ApiOperation(value = "根据分类获取作物列表")
    public BaseResponse<List<CropDTO>> getCropsByCategory(@PathVariable String category) {
        if (FarmCropCategoryEnum.fromValue(category) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int farmLevel = getCurrentFarmLevel();
        return ResultUtils.success(cropService.toDTOList(cropService.getCropsByCategory(category), farmLevel));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据ID获取作物详情")
    public BaseResponse<CropDTO> getCropById(@PathVariable Long id) {
        FarmCrop crop = cropService.getCropById(id);
        if (crop == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(cropService.toDTO(crop, getCurrentFarmLevel()));
    }

    private int getCurrentFarmLevel() {
        Long userId = StpUtil.getLoginIdAsLong();
        FarmUser farmUser = farmUserService.getOrCreateFarmUser(userId);
        return farmUser.getLevel() != null ? farmUser.getLevel() : 1;
    }

    @GetMapping("/categories")
    @ApiOperation(value = "获取所有作物分类")
    public BaseResponse<List<CropCategoryVO>> getCategories() {
        return ResultUtils.success(cropService.getCategories());
    }
}
