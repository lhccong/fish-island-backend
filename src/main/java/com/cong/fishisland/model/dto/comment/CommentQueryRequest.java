package com.cong.fishisland.model.dto.comment;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论查询请求
 * @author 许林涛
 * @date 2025年07月04日 11:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentQueryRequest extends PageRequest {
    /**
     * 帖子 id
     */
    private Long postId;
}
