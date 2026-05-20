package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.dto.farm.request.HarvestRequest;
import com.cong.fishisland.model.dto.farm.request.PlantRequest;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.enums.farm.FarmTaskTypeEnum;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmTaskService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/land")
public class LandController {

    @Autowired
    private FarmLandService landService;

    @Autowired
    private FarmTaskService taskService;

    @Autowired
    private FarmUserService farmUserService;

    @GetMapping("/my")
    @ApiOperation(value = "获取我的地块列表")
    public BaseResponse<List<LandDTO>> getMyLands() {
        Long userId = StpUtil.getLoginIdAsLong();
        farmUserService.getOrCreateFarmUser(userId);
        return ResultUtils.success(landService.toDTOList(landService.getLandsByUserId(userId)));
    }

    @PostMapping("/plant")
    @ApiOperation(value = "种植作物")
    public BaseResponse<LandDTO> plant(@RequestBody PlantRequest request) {

        FarmLand land = landService.plant(request.getLandId(), request.getCropId());

//        taskService.updateTaskProgress(farmUserId, FarmTaskTypeEnum.PLANT);

        return ResultUtils.success(landService.toDTO(land));
    }

    @PostMapping("/harvest")
    @ApiOperation(value = "收获作物")
    public BaseResponse<String> harvest(@RequestBody HarvestRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        FarmLand land = landService.harvest(userId, request.getLandId());
        if (land == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        taskService.updateTaskProgress(userId, FarmTaskTypeEnum.HARVEST);

        return ResultUtils.success("收获成功");
    }
}
