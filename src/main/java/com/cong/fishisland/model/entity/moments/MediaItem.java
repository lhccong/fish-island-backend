package com.cong.fishisland.model.entity.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 媒体资源（图片/视频）
 *
 * @author cong
 */
@Data
@ApiModel(value = "MediaItem", description = "媒体资源")
public class MediaItem implements Serializable {

    @ApiModelProperty(value = "类型：image / video")
    private String type;

    @ApiModelProperty(value = "资源 URL")
    private String url;

    @ApiModelProperty(value = "视频封面，仅 video 类型有效")
    private String cover;

    private static final long serialVersionUID = 1L;
}
