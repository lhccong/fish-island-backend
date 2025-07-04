package com.cong.fishisland.service;

import com.cong.fishisland.model.entity.comment.CommentThumb;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.user.User;

/**
* @author 许林涛
* @description 针对表【comment_thumb(评论点赞表)】的数据库操作Service
* @createDate 2025-07-03 15:57:04
*/
public interface CommentThumbService extends IService<CommentThumb> {
    /**
     * 评论点赞/取消点赞
     * @param commentId 评论ID
     * @param loginUser 登录用户
     * @return 点赞变化数（1：点赞，-1：取消点赞，0：操作失败）
     */
    int doCommentThumb(long commentId, User loginUser);

    /**
     * 内部事务方法
     */
    int doCommentThumbInner(long userId, long commentId);
}
