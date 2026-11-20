package com.cong.fishisland.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.admin.BigFishSettingsRequest;
import com.cong.fishisland.service.impl.chat.BigFishSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/big-fish/settings")
@RequiredArgsConstructor
public class BigFishSettingsController {
    private final BigFishSettingsService settings;

    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @GetMapping
    public BaseResponse<Boolean> get() {
        return ResultUtils.success(settings.isEnabled());
    }

    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @PostMapping
    public BaseResponse<Boolean> update(@RequestBody BigFishSettingsRequest request) {
        if (request == null || request.getEnabled() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择大肥鱼的启用状态");
        }
        settings.setEnabled(request.getEnabled());
        return ResultUtils.success(true);
    }
}
