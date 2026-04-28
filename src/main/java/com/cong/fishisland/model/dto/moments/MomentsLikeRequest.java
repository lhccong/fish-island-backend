package com.cong.fishisland.model.dto.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 点赞请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsLikeRequest", description = "朋友圈点赞/取消点赞请求")
public class MomentsLikeRequest implements Serializable {

    @ApiModelProperty(value = "动态ID", required = true)
    private Long momentId;

    private static final long serialVersionUID = 1L;
}
