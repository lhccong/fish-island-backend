package com.cong.fishisland.model.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Linux Do Token 响应
 *
 * @author shing
 * @date 2025/10/09
 */
@Data
public class LinuxDoTokenResponse {

    /**
     * 访问令牌
     * 用于调用 userinfo 接口获取用户信息
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 令牌类型（通常是 "Bearer"）
     * 使用时需要拼接：Authorization: Bearer {access_token}
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * 访问令牌过期时间（秒）
     * 例如：3600 表示令牌在 1 小时后过期
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * 刷新令牌
     * 用于在访问令牌过期后获取新的访问令牌
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 授权范围
     * 例如："user" 或 "profile"
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * 错误代码（如果请求失败）
     * 例如："invalid_grant", "invalid_client" 等
     */
    @JsonProperty("error")
    private String error;

    /**
     * 错误描述（如果请求失败）
     * 提供错误的详细信息
     */
    @JsonProperty("error_description")
    private String errorDescription;
}

