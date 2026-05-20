package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.CollectionDTO;
import com.cong.fishisland.model.entity.farm.FarmCollection;
import com.cong.fishisland.model.entity.farm.FarmCrop;
import com.cong.fishisland.service.FarmCollectionService;
import com.cong.fishisland.service.FarmCropService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/collection")
public class CollectionController {

    @Autowired
    private FarmCollectionService collectionService;

    @Autowired
    private FarmCropService cropService;

    @Autowired
    private FarmUserService farmUserService;

    @GetMapping("/my")
    @ApiOperation(value = "获取我的收集册信息")
    public BaseResponse<List<CollectionDTO>> getMyCollections() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        List<FarmCollection> collections = collectionService.getUserCollections(farmUserId);
        List<CollectionDTO> dtos = collections.stream().map(this::convertToDTO).collect(Collectors.toList());
        return ResultUtils.success(dtos);
    }

    @GetMapping("/stats")
    @ApiOperation(value = "获取收集册统计信息")
    public BaseResponse<Map<String, Object>> getCollectionStats() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        Map<String, Object> stats = new HashMap<>();
        long obtained = collectionService.getObtainedCount(farmUserId);
        long total = cropService.getAllCrops().size();
        stats.put("obtained", obtained);
        stats.put("total", total);
        stats.put("progress", total > 0 ? (obtained * 100 / total) : 0);
        return ResultUtils.success(stats);
    }

    private CollectionDTO convertToDTO(FarmCollection collection) {
        CollectionDTO dto = new CollectionDTO();
        dto.setId(collection.getId());
        dto.setCropId(collection.getCropId());
        dto.setObtained(collection.getObtained());
        dto.setCount(collection.getCount());
        dto.setObtainedTime(collection.getObtainedTime());

        FarmCrop crop = cropService.getCropById(collection.getCropId());
        if (crop != null) {
            dto.setCropName(crop.getName());
            dto.setCategory(crop.getCategory());
        }

        return dto;
    }
}