package com.cong.fishisland.model.vo;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * IP 地理位置信息（对应 ip-api.com 返回结构）
 */
@Data
@ApiModel("IP地理位置信息")
public class IpLocationVO {

    @ApiModelProperty("查询的 IP 地址")
    private String query;

    @ApiModelProperty("返回状态：success 成功，fail 失败")
    private String status;

    @ApiModelProperty("失败时的错误信息")
    private String message;

    @ApiModelProperty("大陆名称")
    private String continent;

    @ApiModelProperty("大陆代号")
    private String continentCode;

    @ApiModelProperty("国家名称")
    private String country;

    @ApiModelProperty("国家代号")
    private String countryCode;

    @ApiModelProperty("区域")
    private String region;

    @ApiModelProperty("地区/省份")
    private String regionName;

    @ApiModelProperty("城市")
    private String city;

    @ApiModelProperty("区县")
    private String district;

    @ApiModelProperty("邮编")
    private String zip;

    @ApiModelProperty("纬度")
    private Float lat;

    @ApiModelProperty("经度")
    private Float lon;

    @ApiModelProperty("时区")
    private String timezone;

    @ApiModelProperty("本国货币")
    private String currency;

    @ApiModelProperty("互联网服务提供商")
    private String isp;

    @ApiModelProperty("组织名称")
    private String org;

    @JSONField(name = "as")
    @ApiModelProperty("AS 编号")
    private String asNumber;

    @ApiModelProperty("AS 名称")
    private String asname;
}
