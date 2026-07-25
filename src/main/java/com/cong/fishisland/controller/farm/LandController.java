package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.dto.farm.request.HarvestRequest;
import com.cong.fishisland.model.dto.farm.request.PlantRequest;
import com.cong.fishisland.model.dto.farm.request.UnlockLandRequest;
import com.cong.fishisland.model.entity.farm.FarmLand;
import com.cong.fishisland.model.enums.farm.FarmTaskTypeEnum;
import com.cong.fishisland.service.FarmLandService;
import com.cong.fishisland.service.FarmTaskService;
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


    @GetMapping("/my")
    @ApiOperation(value = "获取我的地块列表")
    public BaseResponse<List<LandDTO>> getMyLands() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(landService.toDTOList(landService.getLandsByUserId(userId)));
    }

    @PostMapping("/unlock")
    @ApiOperation(value = "按农场等级解锁地块", notes = "第1–8块默认解锁；第9–12块需达到对应等级、消耗可用积分，且按顺序解锁")
    public BaseResponse<LandDTO> unlock(@RequestBody UnlockLandRequest request) {
        if (request == null || request.getLandId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地块ID不能为空");
        }
        FarmLand land = landService.unlockLand(request.getLandId());
        return ResultUtils.success(landService.toDTO(land));
    }

    @PostMapping("/plant")
    @ApiOperation(value = "批量种植作物")
    public BaseResponse<List<LandDTO>> plant(@RequestBody PlantRequest request) {
        List<FarmLand> lands = landService.plantBatch(request.getItems());

        taskService.updateTaskProgress(FarmTaskTypeEnum.PLANT);

        return ResultUtils.success(landService.toDTOList(lands));
    }

    @PostMapping("/harvest")
    @ApiOperation(value = "批量收获作物")
    public BaseResponse<List<LandDTO>> harvest(@RequestBody HarvestRequest request) {
        List<FarmLand> lands = landService.harvestBatch(request.getLandIds());

        for (int i = 0; i < lands.size(); i++) {
            taskService.updateTaskProgress(FarmTaskTypeEnum.HARVEST);
        }

        return ResultUtils.success(landService.toDTOList(lands));
    }
}
