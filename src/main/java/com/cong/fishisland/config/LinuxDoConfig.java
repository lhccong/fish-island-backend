package com.cong.fishisland.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

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

    /**
     * 代理地址（可选）
     * 格式：http://host:port 或 http://username:password@host:port
     * 示例：
     *   - 无密码代理: http://127.0.0.1:7890
     *   - 有密码代理: http://user:pass@127.0.0.1:7890
     */
    private String proxyUrl;

    /**
     * 判断是否配置了代理
     */
    public boolean hasProxy() {
        return proxyUrl != null && !proxyUrl.isEmpty();
    }

    /**
     * 获取代理主机
     */
    public String getProxyHost() {
        if (!hasProxy()) {
            return null;
        }
        try {
            URI uri = new URI(proxyUrl);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取代理端口
     */
    public int getProxyPort() {
        if (!hasProxy()) {
            return -1;
        }
        try {
            URI uri = new URI(proxyUrl);
            return uri.getPort();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 判断代理是否需要认证
     */
    public boolean hasProxyAuth() {
        if (!hasProxy()) {
            return false;
        }
        try {
            URI uri = new URI(proxyUrl);
            return uri.getUserInfo() != null && uri.getUserInfo().contains(":");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取代理用户名
     */
    public String getProxyUsername() {
        if (!hasProxyAuth()) {
            return null;
        }
        try {
            URI uri = new URI(proxyUrl);
            String userInfo = uri.getUserInfo();
            return userInfo.split(":")[0];
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取代理密码
     */
    public String getProxyPassword() {
        if (!hasProxyAuth()) {
            return null;
        }
        try {
            URI uri = new URI(proxyUrl);
            String userInfo = uri.getUserInfo();
            String[] parts = userInfo.split(":", 2);
            return parts.length > 1 ? parts[1] : null;
        } catch (Exception e) {
            return null;
        }
    }
}

