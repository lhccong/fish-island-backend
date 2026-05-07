package com.cong.fishisland.service.auth;

import com.cong.fishisland.model.vo.auth.OAuth2TokenVO;
import com.cong.fishisland.model.vo.auth.OAuth2UserInfoVO;

/**
 * OAuth2 授权服务端服务
 *
 * @author cong
 */
public interface OAuth2Service {

    /**
     * 校验客户端并生成授权码
     *
     * @param clientId    客户端 ID
     * @param redirectUri 回调地址
     * @param scope       授权范围
     * @param state       状态参数（防 CSRF）
     * @return 带授权码的回调 URL
     */
    String authorize(String clientId, String redirectUri, String scope, String state);

    /**
     * 使用授权码换取 access_token
     *
     * @param clientId     客户端 ID
     * @param clientSecret 客户端密钥
     * @param code         授权码
     * @param redirectUri  回调地址
     * @return token 信息
     */
    OAuth2TokenVO getToken(String clientId, String clientSecret, String code, String redirectUri);

    /**
     * 使用 access_token 获取用户信息
     *
     * @param accessToken access_token
     * @return 用户信息
     */
    OAuth2UserInfoVO getUserInfo(String accessToken);
}
