package com.cong.fishisland.model.vo.moments;

import com.cong.fishisland.model.entity.moments.MediaItem;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 朋友圈动态 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsVO", description = "朋友圈动态")
public class MomentsVO implements Serializable {

    @ApiModelProperty(value = "动态ID")
    private Long id;

    @ApiModelProperty(value = "发布者用户ID")
    private Long userId;

    @ApiModelProperty(value = "发布者昵称")
    private String userName;

    @ApiModelProperty(value = "发布者头像")
    private String userAvatar;

    @ApiModelProperty(value = "文字内容")
    private String content;

    @ApiModelProperty(value = "媒体资源列表（图片/视频）")
    private List<MediaItem> mediaJson;

    @ApiModelProperty(value = "位置信息")
    private String location;

    @ApiModelProperty(value = "可见范围：0-所有朋友，1-仅自己，2-部分可见，3-不给谁看")
    private Integer visibility;

    @ApiModelProperty(value = "点赞数")
    private Integer likeNum;

    @ApiModelProperty(value = "评论数")
    private Integer commentNum;

    @ApiModelProperty(value = "当前登录用户是否已点赞")
    private Boolean liked;

    @ApiModelProperty(value = "发布时间")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
