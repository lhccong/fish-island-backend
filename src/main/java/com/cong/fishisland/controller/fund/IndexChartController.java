package com.cong.fishisland.controller.fund;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.fund.IndexChartVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.fund.IndexChartService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;

@RestController
@RequestMapping("/index")
public class IndexChartController {
    @Resource private IndexChartService chartService;
    @Resource private UserService userService;

    @SaCheckLogin
    @GetMapping("/chart")
    @ApiOperation("指数真实日K与当前用户范围内成交标记；days仅支持60/120/250")
    public BaseResponse<IndexChartVO> chart(@RequestParam String indexCode,
                                            @RequestParam(defaultValue = "60") int days) {
        return ResultUtils.success(chartService.chart(userService.getLoginUser().getId(), indexCode, days));
    }
}
