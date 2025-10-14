package com.cong.fishisland.model.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Linux Do 用户信息响应
 * 对应 Linux Do OAuth2 userinfo 端点返回的数据
 *
 * @author shing
 * @date 2025/10/09
 */
@Data
public class LinuxDoUserInfo {

    /**
     * 用户唯一标识（不可变，数字ID）
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 论坛用户名（用于生成 userAccount）
     */
    @JsonProperty("username")
    private String username;

    /**
     * 论坛用户昵称（可变，用于 userName）
     */
    @JsonProperty("name")
    private String name;

    /**
     * 用户头像模板 URL（用于 userAvatar）
     */
    @JsonProperty("avatar_template")
    private String avatarTemplate;

    /**
     * 电子邮件
     */
    @JsonProperty("email")
    private String email;

    /**
     * 账号活跃状态
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * 信任等级（0-4）
     */
    @JsonProperty("trust_level")
    private Integer trustLevel;

    /**
     * 禁言状态
     */
    @JsonProperty("silenced")
    private Boolean silenced;

    /**
     * 外部 ID 关联信息
     */
    @JsonProperty("external_ids")
    private Object externalIds;

    /**
     * API 访问密钥
     */
    @JsonProperty("api_key")
    private String apiKey;
}

