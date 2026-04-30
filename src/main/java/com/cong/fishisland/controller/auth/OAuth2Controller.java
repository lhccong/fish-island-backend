package com.cong.fishisland.controller.auth;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.auth.OAuth2TokenVO;
import com.cong.fishisland.model.vo.auth.OAuth2UserInfoVO;
import com.cong.fishisland.service.auth.OAuth2Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



/**
 * OAuth2 授权服务端接口
 * <p>
 * 标准授权码流程：
 * 1. GET  /oauth2/authorize  → 用户授权，重定向到 redirect_uri?code=xxx
 * 2. POST /oauth2/token      → 用授权码换 access_token
 * 3. GET  /oauth2/userinfo   → 用 access_token 获取用户信息
 *
 * @author cong
 */
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
//@Api(tags = "OAuth2 授权服务端")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    /**
     * 授权端点：用户同意授权后，重定向到 redirect_uri 并携带授权码
     * <p>
     * 调用前用户必须已登录（Sa-Token session）。
     * 第三方应用将用户浏览器引导至此地址：
     * GET /api/oauth2/authorize?client_id=xxx&redirect_uri=xxx&response_type=code&scope=read&state=xxx
     */
    @GetMapping("/authorize")
    @ApiOperation("授权端点（用户已登录后调用，返回携带授权码的回调地址）")
    public BaseResponse<String> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "response_type", defaultValue = "code") String responseType,
            @RequestParam(value = "scope", defaultValue = "read") String scope,
            @RequestParam(value = "state", required = false) String state) {
        String callbackUrl = oAuth2Service.authorize(clientId, redirectUri, scope, state);
        return ResultUtils.success(callbackUrl);
    }

    /**
     * Token 端点：使用授权码换取 access_token
     * <p>
     * POST /api/oauth2/token
     * 参数（form 或 query）：
     *   grant_type=authorization_code
     *   client_id=xxx
     *   client_secret=xxx
     *   code=xxx
     *   redirect_uri=xxx
     */
    @PostMapping("/token")
    @ApiOperation("Token 端点（授权码换 access_token）")
    public BaseResponse<OAuth2TokenVO> token(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "grant_type", defaultValue = "authorization_code") String grantType) {
        return ResultUtils.success(oAuth2Service.getToken(clientId, clientSecret, code, redirectUri));
    }

    /**
     * 用户信息端点
     * <p>
     * GET /api/oauth2/userinfo
     * Header: Authorization: Bearer {access_token}
     */
    @GetMapping("/userinfo")
    @ApiOperation("用户信息端点（Bearer Token）")
    public BaseResponse<OAuth2UserInfoVO> userInfo(
            @RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "").trim();
        return ResultUtils.success(oAuth2Service.getUserInfo(accessToken));
    }
}
