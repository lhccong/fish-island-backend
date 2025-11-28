package com.cong.fishisland.service.impl.user;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.LinuxDoConfig;
import com.cong.fishisland.model.dto.oauth.LinuxDoTokenResponse;
import com.cong.fishisland.model.dto.oauth.LinuxDoUserInfo;
import com.cong.fishisland.service.LinuxDoOAuth2Service;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * Linux Do OAuth2 服务实现
 * 按照标准 OAuth2 流程实现：
 * 1. 生成授权链接
 * 2. 使用授权码获取访问令牌
 * 3. 使用访问令牌获取用户信息
 *
 * @author shing
 * @date 2025/10/09
 */
@Service
@Slf4j
public class LinuxDoOAuth2ServiceImpl implements LinuxDoOAuth2Service {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private LinuxDoConfig linuxDoConfig;

    /**
     * 第一步：生成授权链接
     * get_auth_url()
     * 使用 OkHttp HttpUrl 构建 URL
     */
    @Override
    public String getAuthorizationUrl(String state) {
        // 使用 OkHttp 的 HttpUrl 构建 URL
        HttpUrl httpUrl = HttpUrl.parse(linuxDoConfig.getAuthUrl());

        if (httpUrl == null) {
            log.error("无效的授权 URL: {}", linuxDoConfig.getAuthUrl());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OAuth2 配置错误：授权 URL 格式不正确");
        }

        HttpUrl url = httpUrl.newBuilder()
                .addQueryParameter("client_id", linuxDoConfig.getClientId())
//                .addQueryParameter("redirect_uri", linuxDoConfig.getRedirectUri())
                .addQueryParameter("response_type", "code")
                .addQueryParameter("scope", "user")
                .addQueryParameter("state", state)
                .build();

        return url.toString();
    }

    /**
     * 第二步：使用授权码获取访问令牌
     * get_access_token(code)
     */
    @Override
    public LinuxDoTokenResponse getAccessToken(String code) {
        try {
            // 构建请求参数（根据 Python 代码，参数放在请求体中）
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", linuxDoConfig.getClientId());
            params.add("client_secret", linuxDoConfig.getClientSecret());
            params.add("code", code);
            params.add("redirect_uri", linuxDoConfig.getRedirectUri());
            params.add("grant_type", "authorization_code");

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // 添加 Accept 头
            headers.set("Accept", "application/json");
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            // 发送 POST 请求
            ResponseEntity<LinuxDoTokenResponse> response = restTemplate.exchange(
                    linuxDoConfig.getTokenUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    LinuxDoTokenResponse.class
            );
            LinuxDoTokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取访问令牌失败：响应为空");
            }

            return tokenResponse;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取访问令牌失败: " + e.getMessage());
        }
    }

    /**
     * 第三步：使用访问令牌获取用户信息
     * get_user_info(access_token)
     */
    @Override
    public LinuxDoUserInfo getUserInfo(String accessToken) {
        try {
            // 设置请求头，添加 Bearer Token
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            // 发送 GET 请求获取用户信息
            ResponseEntity<LinuxDoUserInfo> response = restTemplate.exchange(
                    linuxDoConfig.getUserInfoUrl(),
                    HttpMethod.GET,
                    requestEntity,
                    LinuxDoUserInfo.class
            );

            LinuxDoUserInfo userInfo = response.getBody();

            if (userInfo == null) {
                log.error("用户信息响应为空");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败：响应为空");
            }
            return userInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 刷新访问令牌
     */
    @Override
    public LinuxDoTokenResponse refreshAccessToken(String refreshToken) {
        try {
            // 构建请求参数
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", linuxDoConfig.getClientId());
            params.add("client_secret", linuxDoConfig.getClientSecret());
            params.add("refresh_token", refreshToken);
            params.add("grant_type", "refresh_token");

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            // 发送 POST 请求
            ResponseEntity<LinuxDoTokenResponse> response = restTemplate.exchange(
                    linuxDoConfig.getTokenUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    LinuxDoTokenResponse.class
            );

            LinuxDoTokenResponse tokenResponse = response.getBody();

            if (tokenResponse == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "刷新访问令牌失败：响应为空");
            }

            return tokenResponse;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "刷新访问令牌失败: " + e.getMessage());
        }
    }
}

