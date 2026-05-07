package com.cong.fishisland.model.vo.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 评论 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsCommentVO", description = "朋友圈评论")
public class MomentsCommentVO implements Serializable {

    @ApiModelProperty(value = "评论ID")
    private Long id;

    @ApiModelProperty(value = "动态ID")
    private Long momentId;

    @ApiModelProperty(value = "评论者用户ID")
    private Long userId;

    @ApiModelProperty(value = "评论者昵称")
    private String userName;

    @ApiModelProperty(value = "评论者头像")
    private String userAvatar;

    @ApiModelProperty(value = "被回复的用户ID")
    private Long replyUserId;

    @ApiModelProperty(value = "被回复的用户昵称")
    private String replyUserName;

    @ApiModelProperty(value = "父评论ID，为空表示顶级评论")
    private Long parentId;

    @ApiModelProperty(value = "评论内容")
    private String content;

    @ApiModelProperty(value = "评论时间")
    private Date createTime;

    @ApiModelProperty(value = "子评论列表（仅顶级评论携带）")
    private List<MomentsCommentVO> children;

    private static final long serialVersionUID = 1L;
}
