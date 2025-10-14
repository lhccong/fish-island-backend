package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Linux Do OAuth2 配置
 *
 * @author shing
 * @date 2025/10/09
 */
@Configuration
@ConfigurationProperties(prefix = "linux-do")
@Data
public class LinuxDoConfig {

    /**
     * Client ID
     */
    private String clientId;

    /**
     * Client Secret
     */
    private String clientSecret;

    /**
     * 回调地址
     */
    private String redirectUri;

    /**
     * 授权 URL
     */
    private String authUrl;

    /**
     * Token URL
     */
    private String tokenUrl;

    /**
     * 用户信息 URL
     */
    private String userInfoUrl;
}

