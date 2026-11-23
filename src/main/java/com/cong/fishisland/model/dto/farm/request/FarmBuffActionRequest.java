package com.cong.fishisland.model.dto.farm.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 农场 buff 购买/升级请求
 */
@Data
@ApiModel(description = "农场 buff 购买/升级请求")
public class FarmBuffActionRequest {

    @ApiModelProperty(value = "道具类型：1-生长加速 2-积分增产 3-守护 4-神奇生长", required = true)
    private Integer buffType;
}
