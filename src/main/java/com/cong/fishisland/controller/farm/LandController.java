package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.dto.farm.request.HarvestRequest;
import com.cong.fishisland.model.dto.farm.request.PlantRequest;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.service.FarmCropService;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmTaskService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/land")
public class LandController {

    @Autowired
    private FarmLandService landService;

    @Autowired
    private FarmCropService cropService;

    @Autowired
    private FarmTaskService taskService;

    @Autowired
    private FarmUserService farmUserService;

    @GetMapping("/my")
    @ApiOperation(value = "获取我的地块列表")
    public BaseResponse<List<LandDTO>> getMyLands() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        List<FarmLand> lands = landService.getLandsByUserId(farmUserId);
        List<LandDTO> dtos = lands.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResultUtils.success(dtos);
    }

    @PostMapping("/plant")
    @ApiOperation(value = "种植作物")
    public BaseResponse<LandDTO> plant(@RequestBody PlantRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        FarmLand land = landService.plant(farmUserId, request.getLandId(), request.getCropId());
        if (land == null) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR);
        }

        taskService.updateTaskProgress(farmUserId, "plant");

        return ResultUtils.success(convertToDTO(land));
    }

    @PostMapping("/harvest")
    @ApiOperation(value = "收获作物")
    public BaseResponse<String> harvest(@RequestBody HarvestRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        FarmLand land = landService.harvest(farmUserId, request.getLandId());
        if (land == null) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR);
        }

        taskService.updateTaskProgress(farmUserId, "harvest");

        return ResultUtils.success("收获成功");
    }

    private LandDTO convertToDTO(FarmLand land) {
        LandDTO dto = new LandDTO();
        dto.setId(land.getId());
        dto.setLandIndex(land.getLandIndex());
        dto.setStatus(land.getStatus());
        dto.setPlantedCropId(land.getPlantedCropId());
        dto.setPlantedTime(land.getPlantedTime());
        dto.setHarvestTime(land.getHarvestTime());
        dto.setLocked(land.getLocked());

        if (land.getPlantedCropId() != null) {
            FarmCrop crop = cropService.getCropById(land.getPlantedCropId());
            if (crop != null) {
                dto.setCropName(crop.getName());
            }
        }
        return dto;
    }
}