package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.LandDTO;
import com.cong.fishisland.model.dto.farm.WaterLandResult;
import com.cong.fishisland.model.dto.farm.request.WaterBatchRequest;
import com.cong.fishisland.model.dto.farm.request.HarvestRequest;
import com.cong.fishisland.model.dto.farm.request.PlantRequest;
import com.cong.fishisland.model.dto.farm.request.UnlockLandRequest;
import com.cong.fishisland.model.dto.farm.request.WaterLandRequest;
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

    @PostMapping("/water")
    @ApiOperation(value = "给作物浇水", notes = "仅限自己的未成熟作物，每株每30分钟一次、每天最多10次，剩余成熟时间缩短2%（向上取整到秒）")
    public BaseResponse<LandDTO> water(@RequestBody WaterLandRequest request) {
        if (request == null || request.getLandId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "地块ID不能为空");
        }
        return ResultUtils.success(landService.toDTO(landService.waterLand(request.getLandId())));
    }

    @PostMapping("/water/batch")
    @ApiOperation(value = "批量浇水", notes = "最多24株，每株独立校验与事务；返回各株结果，不影响其他株。冷却30分钟、每日10次、剩余成熟时间缩短2%。")
    public BaseResponse<List<WaterLandResult>> waterBatch(@RequestBody WaterBatchRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "浇水列表不能为空");
        }
        return ResultUtils.success(landService.waterBatch(request.getLandIds()));
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
