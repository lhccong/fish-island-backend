package com.cong.fishisland.model.dto.comment;

import lombok.Data;

/**
 * @author 许林涛
 * @date 2025年07月04日 10:33
 */
@Data
public class CommentAddRequest {
    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 评论内容
     */
    private String content;
}
