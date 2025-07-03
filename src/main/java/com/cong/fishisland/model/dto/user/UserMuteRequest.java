package com.cong.fishisland.model.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户禁言请求
 *
 * @author cong
 */
@Data
//@ApiModel(value = "用户禁言请求")
public class UserMuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @ApiModelProperty(value = "用户id")
    private Long userId;

    /**
     * 禁言时间（秒）
     */
    @ApiModelProperty(value = "禁言时间（秒）")
    private Long duration;
} 