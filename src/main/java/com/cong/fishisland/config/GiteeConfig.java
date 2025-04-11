package com.cong.fishisland.config;

import com.cong.fishisland.cache.AuthStateRedisCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Git Hub 配置
 *
 * @author cong
 * @date 2024/04/22
 */
@Configuration
public class GiteeConfig {

    @Resource
    private AuthStateRedisCache stateRedisCache;

    @Value("${gitee.client-id}")
    private String clientId;

    @Value("${gitee.client-secret}")
    private String clientSecret;

    @Value("${gitee.redirect-uri}")
    private String redirectUri;


    public AuthRequest getAuthRequest(){
        return new AuthGiteeRequest(AuthConfig.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                // .scopes(AuthScopeUtils.getScopes(AuthGithubScope.values()))
                .build(), stateRedisCache);
    }
}
