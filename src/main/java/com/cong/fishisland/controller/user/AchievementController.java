package com.cong.fishisland.controller.user;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.service.achievement.AchievementService;
import com.cong.fishisland.service.achievement.AchievementService.Summary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/achievements")
@RequiredArgsConstructor
public class AchievementController {
    private final AchievementService achievements;

    @GetMapping
    public BaseResponse<Summary> list() {
        return ResultUtils.success(achievements.list(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/claim")
    public BaseResponse<Summary> claim() {
        return ResultUtils.success(achievements.claim(StpUtil.getLoginIdAsLong()));
    }
}
