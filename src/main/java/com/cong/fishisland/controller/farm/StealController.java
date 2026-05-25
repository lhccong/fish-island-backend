package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.dto.farm.request.StealRequest;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import com.cong.fishisland.service.FarmStealService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/steal")
public class StealController {

    @Autowired
    private FarmStealService stealService;

    @PostMapping
    @ApiOperation(value = "偷菜")
    public BaseResponse<FarmStealRecord> steal(@RequestBody StealRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        FarmStealRecord record = stealService.steal(userId, request.getLandId());
        if (record == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "偷菜失败");
        }
        return ResultUtils.success(record);
    }

    @GetMapping("/my-stolen")
    @ApiOperation(value = "谁偷了我的菜")
    public BaseResponse<List<FarmStealRecordVO>> getMyStolenRecords() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FarmStealRecordVO> records = stealService.getStealRecordsByOwner(userId);
        return ResultUtils.success(records);
    }
}