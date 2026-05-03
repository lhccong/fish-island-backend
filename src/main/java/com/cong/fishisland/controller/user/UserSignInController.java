package com.cong.fishisland.controller.user;

import com.cong.fishisland.annotation.NoRepeatSubmit;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.user.MakeUpSignInRequest;
import com.cong.fishisland.model.vo.user.MonthSignInVO;
import com.cong.fishisland.model.vo.user.SignInStatusVO;
import com.cong.fishisland.model.vo.user.SignInVO;
import com.cong.fishisland.service.UserSignInService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 连续签到接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/sign")
@Slf4j
//@Api(tags = "连续签到接口")
public class UserSignInController {

    @Resource
    private UserSignInService userSignInService;

    /**
     * 补签
     * <p>
     * 每月最多补签 3 次，每次消耗 20 积分，只能补签最近 7 天内的未签到日期。
     */
    @PostMapping("/makeup")
    @ApiOperation(value = "补签", notes = "每月最多补签3次，每次消耗20积分，只能补签最近7天内的未签到日期")
    @NoRepeatSubmit
    public BaseResponse<SignInVO> makeUpSignIn(@RequestBody MakeUpSignInRequest request) {
        if (request == null || StringUtils.isBlank(request.getSignDate())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "补签日期不能为空");
        }
        SignInVO vo = userSignInService.makeUpSignIn(request.getSignDate());
        return ResultUtils.success(vo);
    }

    /**
     * 查询签到状态
     * <p>
     * 返回今日是否已签到、连续天数、本周期签到状态、可补签日期列表等。
     */
    @GetMapping("/status")
    @ApiOperation(value = "查询签到状态", notes = "返回今日签到状态、连续天数、本周期进度、可补签日期列表")
    public BaseResponse<SignInStatusVO> getSignInStatus() {
        return ResultUtils.success(userSignInService.getSignInStatus());
    }

    /**
     * 获取月度签到日历
     * <p>
     * 返回指定月份每天的签到状态、奖励积分，以及顶部统计数据（连续天数、累计天数、当前积分、剩余补签卡）。
     * year 和 month 均不传时默认返回当前月。
     */
    @GetMapping("/month")
    @ApiOperation(value = "月度签到日历", notes = "返回指定月份每天签到状态和奖励，year/month 不传默认当前月")
    public BaseResponse<MonthSignInVO> getMonthSignIn(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResultUtils.success(userSignInService.getMonthSignIn(year, month));
    }
}
