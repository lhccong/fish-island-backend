package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.oauth.LinuxDoTokenResponse;
import com.cong.fishisland.model.dto.oauth.LinuxDoUserInfo;

/**
 * Linux Do OAuth2 服务接口
 * 实现标准 OAuth2 授权流程
 *
 * @author shing
 * @date 2025/10/09
 */
public interface LinuxDoOAuth2Service {

    /**
     * 第一步：生成授权链接
     * 
     * @param state 状态参数，用于防止 CSRF 攻击
     * @return 授权链接 URL
     */
    String getAuthorizationUrl(String state);

    /**
     * 第二步：使用授权码获取访问令牌
     * 
     * @param code 授权码
     * @return Token 响应
     */
    LinuxDoTokenResponse getAccessToken(String code);

    /**
     * 第三步：使用访问令牌获取用户信息
     * 
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    LinuxDoUserInfo getUserInfo(String accessToken);

    /**
     * 刷新访问令牌
     * 
     * @param refreshToken 刷新令牌
     * @return 新的 Token 响应
     */
    LinuxDoTokenResponse refreshAccessToken(String refreshToken);
}

