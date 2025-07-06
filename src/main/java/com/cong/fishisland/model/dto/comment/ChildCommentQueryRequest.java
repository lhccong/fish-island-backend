package com.cong.fishisland.model.dto.comment;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 子评论查询请求
 * @author 许林涛
 * @date 2025年07月04日 11:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChildCommentQueryRequest  extends PageRequest {
    /**
     * 根评论id
     */
    private Long rootId;
}