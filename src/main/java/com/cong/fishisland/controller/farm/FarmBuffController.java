package com.cong.fishisland.controller.farm;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.farm.FarmBuffVO;
import com.cong.fishisland.model.dto.farm.request.FarmBuffActionRequest;
import com.cong.fishisland.model.entity.farm.FarmUserBuff;
import com.cong.fishisland.service.FarmUserBuffService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/buff")
public class FarmBuffController {

    @Resource
    private FarmUserBuffService farmUserBuffService;

    @GetMapping("/my")
    @ApiOperation(value = "我的农场 buff 道具状态", notes = "商城/背包/农田左侧共用：含持有、等级、解锁条件、消耗与门槛")
    public BaseResponse<List<FarmBuffVO>> getMyBuffs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultUtils.success(farmUserBuffService.getMyBuffs(userId));
    }

    @PostMapping("/purchase")
    @ApiOperation(value = "购买农场 buff 道具", notes = "校验田地数/农场等级解锁条件，购买后自动全局生效")
    public BaseResponse<FarmUserBuff> purchase(@RequestBody FarmBuffActionRequest request) {
        if (request == null || request.getBuffType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "道具类型不能为空");
        }
        return ResultUtils.success(farmUserBuffService.purchase(request.getBuffType()));
    }

    @PostMapping("/upgrade")
    @ApiOperation(value = "升级农场 buff 道具", notes = "需拥有品级 ≥ 目标等级的图鉴记录（不消耗）+ 积分；加速道具升级后对生长中作物实时生效")
    public BaseResponse<FarmUserBuff> upgrade(@RequestBody FarmBuffActionRequest request) {
        if (request == null || request.getBuffType() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "道具类型不能为空");
        }
        return ResultUtils.success(farmUserBuffService.upgrade(request.getBuffType()));
    }
}
