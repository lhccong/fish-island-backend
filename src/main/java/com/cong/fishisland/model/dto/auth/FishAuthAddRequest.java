package com.cong.fishisland.model.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建第三方应用请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "FishAuthAddRequest", description = "创建第三方应用请求")
public class FishAuthAddRequest implements Serializable {

    @ApiModelProperty(value = "应用名称", required = true)
    private String appName;

    @ApiModelProperty(value = "应用网站地址")
    private String appWebsite;

    @ApiModelProperty(value = "应用描述")
    private String appDesc;

    @ApiModelProperty(value = "回调地址（多个用逗号分隔）", required = true)
    private String redirectUri;

    private static final long serialVersionUID = 1L;
}
