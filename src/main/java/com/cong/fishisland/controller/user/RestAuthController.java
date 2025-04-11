package com.cong.fishisland.controller.user;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.config.GitHubConfig;
import com.cong.fishisland.config.GiteeConfig;
import com.cong.fishisland.constant.Oauth2Constant;
import com.cong.fishisland.constant.SystemConstants;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserThirdAuth;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserThirdAuthService;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/oauth")
@Slf4j
public class RestAuthController {

    @Resource
    private GitHubConfig gitHubConfig;

    @Resource
    private GiteeConfig giteeConfig;

    @Resource
    private UserThirdAuthService userThirdAuthService;

    @Resource
    private UserService userService;

    private final Map<String, AuthRequest> authRequests = new HashMap<>(8);

    @PostConstruct
    public void init() {
        authRequests.put(Oauth2Constant.GITHub, gitHubConfig.getAuthRequest());
        authRequests.put(Oauth2Constant.GITEE, giteeConfig.getAuthRequest());
        // authRequests.put(Oauth2Constant.QQ, xxx);
    }

    /**
     * render auth
     *
     * @param source 授权平台
     */
    @GetMapping("/render/{source}")
    @ResponseBody
    public BaseResponse<String> renderAuth(@PathVariable String source) {
        AuthRequest authRequest = authRequests.get(source);
        if (null == authRequest) {
            throw new AuthException("未获取到有效的Auth配置");
        }
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return ResultUtils.success(authorizeUrl);
    }

    /**
     * oauth平台中配置的授权回调地址
     */
    @RequestMapping("/callback/{source}")
    @ResponseBody
    public ModelAndView callback(@PathVariable("source") String source, AuthCallback callback) throws Exception {
        AuthRequest authRequest = authRequests.get(source);
        // 生产改为实际地址，建议放到配置中 TODO
        String frontUrl = "https://fish.codebug.icu";
        if (null == authRequest) {
            throw new AuthException("未获取到有效的Auth配置");
        }
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (response.ok()) {
            AuthUser rowData = response.getData();
            // 检查第三方账号是否已存在
            UserThirdAuth thirdAuth = userThirdAuthService.getThirdAuth(rowData.getUuid(), source);
            Long userId;
            if (null == thirdAuth) {
                // 当前用户是否已经登录
                boolean isLogin = StpUtil.isLogin();
                if (isLogin) {
                    // 当前用户已经登录，则绑定第三方账号
                    userId = StpUtil.getLoginIdAsLong();
                } else {
                    // 当前用户不存在，则创建用户
                    userId = userThirdAuthService.userRegister(rowData);
                }
            } else {
                // 当前用户是否已经登录
                boolean isLogin = StpUtil.isLogin();
                if (isLogin) {
                    // 是否是该三方账号绑定的用户
                    if (thirdAuth.getUserId() != StpUtil.getLoginIdAsLong()) {
                        // 处理错误情况
                        String errorRedirectUrl = StrUtil.format("{}/auth-callback?error=" + URLEncoder.encode("该平台账号已经绑定过用户！", "UTF-8"), frontUrl);
                        return new ModelAndView("redirect:" + errorRedirectUrl);
                    }
                }
                userId = thirdAuth.getUserId();
                StpUtil.login(userId);
                User user = userService.getById(userId);
                StpUtil.getTokenSession().set(SystemConstants.USER_LOGIN_STATE, user);
            }

            // 保存第三方账号信息
            userThirdAuthService.saveOrUpdateThirdAuth(userId, source, rowData);

            // TokenLoginUserVo tokenLoginUserVo = userThirdAuthService.getTokenLoginUserVO(userId);

            String tokenName = StpUtil.getTokenName();
            String tokenValue = StpUtil.getTokenValue();
            // 构建重定向URL，将 token 信息作为参数传递
            String redirectUrl = StrUtil.format(
                    "{}/auth-callback?tokenName={}&tokenValue={}&source={}",
                    frontUrl,
                    URLEncoder.encode(tokenName, "UTF-8"),
                    URLEncoder.encode(tokenValue, "UTF-8"),
                    URLEncoder.encode(source, "UTF-8")
            );
            return new ModelAndView("redirect:" + redirectUrl);
        }
        // 处理错误情况
        String errorRedirectUrl = StrUtil.format("{}/auth-callback?error=" + URLEncoder.encode(response.getMsg(), "UTF-8"), frontUrl);
        return new ModelAndView("redirect:" + errorRedirectUrl);
    }


    /**
     * 解绑三方平台
     *
     * @param source 平台类型
     * @return {@link BaseResponse }<{@link Boolean }>
     */
    @DeleteMapping("/unbind/{source}")
    @ResponseBody
    public BaseResponse<Boolean> unbind(@PathVariable String source) {
        return ResultUtils.success(userThirdAuthService.unbind(source));
    }
}
