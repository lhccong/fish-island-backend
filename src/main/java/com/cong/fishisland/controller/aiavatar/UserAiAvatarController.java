package com.cong.fishisland.controller.aiavatar;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.aiavatar.UserAiAvatarGenerateRequest;
import com.cong.fishisland.model.dto.aiavatar.UserAiAvatarSaveRequest;
import com.cong.fishisland.model.entity.aiavatar.UserAiAvatar;
import com.cong.fishisland.model.vo.aiavatar.UserAiAvatarGenerateVO;
import com.cong.fishisland.service.UserAiAvatarService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 AI 分身控制器
 * @author cong
 */
@RestController
@RequestMapping("/userAiAvatar")
@Slf4j
@RequiredArgsConstructor
public class UserAiAvatarController {

    private final UserAiAvatarService userAiAvatarService;

    @PostMapping("/save")
    @ApiOperation(value = "保存分身配置")
    public BaseResponse<Boolean> saveAvatar(@RequestBody UserAiAvatarSaveRequest request) {
        checkLogin();
        return ResultUtils.success(userAiAvatarService.saveAvatar(request));
    }

    @GetMapping("/get")
    @ApiOperation(value = "获取当前用户分身配置")
    public BaseResponse<UserAiAvatar> getAvatar() {
        checkLogin();
        return ResultUtils.success(userAiAvatarService.getCurrentUserAvatar());
    }

    // @PostMapping("/generatePrompt")
    // @ApiOperation(value = "AI 生成系统提示词")
    // public BaseResponse<UserAiAvatarGenerateVO> generatePrompt(@RequestBody UserAiAvatarGenerateRequest request) {
    //     checkLogin();
    //     return ResultUtils.success(userAiAvatarService.generateSystemPrompt(request));
    // }

    private void checkLogin() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
    }
}
