package com.cong.fishisland.model.dto.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 置顶/取消置顶动态请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsTopRequest", description = "置顶/取消置顶朋友圈动态请求（仅管理员）")
public class MomentsTopRequest implements Serializable {

    @ApiModelProperty(value = "动态ID", required = true)
    private Long momentId;

    @ApiModelProperty(value = "是否置顶：true-置顶，false-取消置顶", required = true)
    private Boolean top;

    private static final long serialVersionUID = 1L;
}
