package com.cong.fishisland.model.dto.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 置顶/取消置顶评论请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsCommentTopRequest", description = "置顶/取消置顶朋友圈评论请求（动态发布者或管理员）")
public class MomentsCommentTopRequest implements Serializable {

    @ApiModelProperty(value = "评论ID", required = true)
    private Long commentId;

    @ApiModelProperty(value = "是否置顶：true-置顶，false-取消置顶", required = true)
    private Boolean top;

    private static final long serialVersionUID = 1L;
}
