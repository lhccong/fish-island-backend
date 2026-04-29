package com.cong.fishisland.model.dto.moments;

import com.cong.fishisland.model.entity.moments.MediaItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发布朋友圈请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsAddRequest", description = "发布朋友圈动态请求")
public class MomentsAddRequest implements Serializable {

    @ApiModelProperty(value = "文字内容")
    private String content;

    @ApiModelProperty(value = "媒体资源列表（图片/视频）")
    private List<MediaItem> mediaJson;

    @ApiModelProperty(value = "位置信息")
    private String location;

    @ApiModelProperty(value = "可见范围：0-所有朋友，1-仅自己，2-部分可见，3-不给谁看", example = "0")
    private Integer visibility;

    @ApiModelProperty(value = "部分可见的用户ID列表（visibility=2时有效）")
    private List<Long> allowList;

    @ApiModelProperty(value = "不给谁看的用户ID列表（visibility=3时有效）")
    private List<Long> blockList;

    private static final long serialVersionUID = 1L;
}
