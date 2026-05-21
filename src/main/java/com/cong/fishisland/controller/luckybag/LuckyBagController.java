package com.cong.fishisland.controller.luckybag;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.luckybag.CreateLuckyBagRequest;
import com.cong.fishisland.model.entity.luckybag.LuckyBag;
import com.cong.fishisland.model.vo.luckybag.LuckyBagRecordVO;
import com.cong.fishisland.service.LuckyBagService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 福袋控制器
 */
@RestController
@RequestMapping("/luckybag")
@Slf4j
@RequiredArgsConstructor
public class LuckyBagController {

    private final LuckyBagService luckyBagService;

    @PostMapping("/create")
    @ApiOperation(value = "创建福袋")
    public BaseResponse<String> createLuckyBag(@RequestBody @Validated CreateLuckyBagRequest request) {
        return ResultUtils.success(luckyBagService.createLuckyBag(request));
    }

    @PostMapping("/join")
    @ApiOperation(value = "参与福袋")
    public BaseResponse<Boolean> joinLuckyBag(
            @RequestParam @ApiParam(value = "福袋ID", required = true) String luckyBagId) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        luckyBagService.joinLuckyBag(luckyBagId, userId);
        return ResultUtils.success(true);
    }

    @GetMapping("/detail")
    @ApiOperation(value = "获取福袋详情")
    public BaseResponse<LuckyBag> getLuckyBagDetail(
            @RequestParam @ApiParam(value = "福袋ID", required = true) String luckyBagId) {
        return ResultUtils.success(luckyBagService.getLuckyBagDetail(luckyBagId));
    }

    @GetMapping("/records")
    @ApiOperation(value = "获取福袋中奖记录")
    public BaseResponse<List<LuckyBagRecordVO>> getLuckyBagWinRecords(
            @RequestParam @ApiParam(value = "福袋ID", required = true) String luckyBagId) {
        return ResultUtils.success(luckyBagService.getLuckyBagWinRecords(luckyBagId));
    }

    @GetMapping("/active")
    @ApiOperation(value = "获取当前进行中的福袋列表")
    public BaseResponse<List<LuckyBag>> getActiveLuckyBags() {
        return ResultUtils.success(luckyBagService.getActiveLuckyBags());
    }
}
