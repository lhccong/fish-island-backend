package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.FarmStealRecordVO;
import com.cong.fishisland.model.dto.farm.request.StealRequest;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.cong.fishisland.model.entity.farm.FarmStealRecord;
import com.cong.fishisland.service.FarmStealService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/steal")
public class StealController {

    @Autowired
    private FarmStealService stealService;

    @PostMapping
    @ApiOperation(value = "偷菜（支持批量）")
    public BaseResponse<List<FarmStealRecord>> steal(@RequestBody StealRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Long> landIds = resolveLandIds(request);
        List<FarmStealRecord> records = stealService.stealBatch(userId, landIds);
        if (CollectionUtils.isEmpty(records)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "偷菜失败");
        }
        return ResultUtils.success(records);
    }

    private List<Long> resolveLandIds(StealRequest request) {
        if (CollectionUtils.isNotEmpty(request.getLandIds())) {
            return request.getLandIds();
        }
        if (request.getLandId() != null) {
            return Collections.singletonList(request.getLandId());
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "地块ID不能为空");
    }

    @GetMapping("/my-stolen")
    @ApiOperation(value = "谁偷了我的菜")
    public BaseResponse<List<FarmStealRecordVO>> getMyStolenRecords() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FarmStealRecordVO> records = stealService.getStealRecordsByOwner(userId);
        return ResultUtils.success(records);
    }
}