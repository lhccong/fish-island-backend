package com.cong.fishisland.model.vo.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 第三方应用 VO（对外展示，隐藏 clientSecret）
 *
 * @author cong
 */
@Data
@ApiModel(value = "FishAuthVO", description = "第三方应用信息")
public class FishAuthVO implements Serializable {

    @ApiModelProperty(value = "应用 ID")
    private Long id;

    @ApiModelProperty(value = "应用名称")
    private String appName;

    @ApiModelProperty(value = "应用网站地址")
    private String appWebsite;

    @ApiModelProperty(value = "应用描述")
    private String appDesc;

    @ApiModelProperty(value = "回调地址")
    private String redirectUri;

    @ApiModelProperty(value = "Client ID")
    private String clientId;

    @ApiModelProperty(value = "状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
