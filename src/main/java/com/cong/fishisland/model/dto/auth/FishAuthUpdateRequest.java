package com.cong.fishisland.model.dto.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新第三方应用请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "FishAuthUpdateRequest", description = "更新第三方应用请求")
public class FishAuthUpdateRequest implements Serializable {

    @ApiModelProperty(value = "应用 ID", required = true)
    private Long id;

    @ApiModelProperty(value = "应用名称")
    private String appName;

    @ApiModelProperty(value = "应用网站地址")
    private String appWebsite;

    @ApiModelProperty(value = "应用描述")
    private String appDesc;

    @ApiModelProperty(value = "回调地址（多个用逗号分隔）")
    private String redirectUri;

    @ApiModelProperty(value = "状态：0-禁用，1-启用")
    private Integer status;

    private static final long serialVersionUID = 1L;
}
