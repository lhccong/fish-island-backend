package com.cong.fishisland.model.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 补签请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MakeUpSignInRequest", description = "补签请求")
public class MakeUpSignInRequest implements Serializable {

    /**
     * 补签日期，格式 yyyy-MM-dd
     */
    @ApiModelProperty(value = "补签日期，格式 yyyy-MM-dd", required = true, example = "2025-05-01")
    private String signDate;

    private static final long serialVersionUID = 1L;
}
