package com.cong.fishisland.model.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户禁言信息
 *
 * @author cong
 */
@Data
//@ApiModel(value = "用户禁言信息")
public class UserMuteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否被禁言
     */
    @ApiModelProperty(value = "是否被禁言")
    private Boolean isMuted;

    /**
     * 剩余禁言时间（格式化为时分秒）
     */
    @ApiModelProperty(value = "剩余禁言时间（格式化为时分秒）")
    private String remainingTime;
} 