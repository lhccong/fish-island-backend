package com.cong.fishisland.model.dto.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 发表评论请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsCommentAddRequest", description = "发表朋友圈评论请求")
public class MomentsCommentAddRequest implements Serializable {

    @ApiModelProperty(value = "动态ID", required = true)
    private Long momentId;

    @ApiModelProperty(value = "被回复的用户ID，为空表示直接评论动态")
    private Long replyUserId;

    @ApiModelProperty(value = "父评论ID，为空表示顶级评论")
    private Long parentId;

    @ApiModelProperty(value = "评论内容", required = true)
    private String content;

    private static final long serialVersionUID = 1L;
}
