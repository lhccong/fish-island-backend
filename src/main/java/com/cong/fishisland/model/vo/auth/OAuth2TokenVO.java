package com.cong.fishisland.model.vo.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * OAuth2 Token 响应
 *
 * @author cong
 */
@Data
@ApiModel(value = "OAuth2TokenVO", description = "OAuth2 Token 响应")
public class OAuth2TokenVO implements Serializable {

    @JsonProperty("access_token")
    @ApiModelProperty(value = "访问令牌")
    private String accessToken;

    @JsonProperty("token_type")
    @ApiModelProperty(value = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @ApiModelProperty(value = "过期时间（秒）")
    private Long expiresIn;

    @JsonProperty("scope")
    @ApiModelProperty(value = "授权范围")
    private String scope;

    private static final long serialVersionUID = 1L;
}
