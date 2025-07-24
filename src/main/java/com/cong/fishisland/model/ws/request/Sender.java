package com.cong.fishisland.model.ws.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author cong
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Sender {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("points")
    private int points;

    @JsonProperty("level")
    private int level;

    @ApiModelProperty(value = "用户简介")
    private String userProfile;

    @JsonProperty("avatarFramerUrl")
    private String avatarFramerUrl;

    @ApiModelProperty(value = "用户称号 ID")
    private Long titleId;

    @ApiModelProperty(value = "用户称号ID列表")
    private String titleIdList;

    @JsonProperty("isAdmin")
    private boolean isAdmin;

    @JsonProperty("isVip")
    private boolean isVip;

    @JsonProperty("region")
    private String region;

    @JsonProperty("country")
    private String country;
}