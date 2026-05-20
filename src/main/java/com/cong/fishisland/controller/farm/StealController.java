package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.dto.farm.request.StealRequest;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import com.cong.fishisland.model.entity.farm.FarmUser;
import com.cong.fishisland.service.FarmStealService;
import com.cong.fishisland.service.FarmUserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/steal")
public class StealController {

    @Autowired
    private FarmStealService stealService;

    @Autowired
    private FarmUserService farmUserService;

    @PostMapping
    @ApiOperation(value = "偷菜")
    public BaseResponse<FarmStealRecord> steal(@RequestBody StealRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        FarmStealRecord record = stealService.steal(farmUserId, request.getPlantRecordId());
        if (record == null) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "偷菜失败");
        }
        return ResultUtils.success(record);
    }

    @GetMapping("/my-stolen")
    @ApiOperation(value = "谁偷了我的菜")
    public BaseResponse<List<FarmStealRecordVO>> getMyStolenRecords() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long farmUserId = farmUserService.getFarmUserId(userId);
        List<FarmStealRecordVO> records = stealService.getStealRecordsByOwner(farmUserId);
        return ResultUtils.success(records);
    }
}