package com.cong.fishisland.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.admin.BroadcastPublishRequest;
import com.cong.fishisland.service.admin.BroadcastService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/broadcast")
public class BroadcastController {
    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @PostMapping("/publish")
    public BaseResponse<Boolean> publish(@RequestBody BroadcastPublishRequest request) {
        broadcastService.publish(request == null ? null : request.getContent());
        return ResultUtils.success(true);
    }
}
