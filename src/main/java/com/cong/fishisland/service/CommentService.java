package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.model.dto.comment.ChildCommentQueryRequest;
import com.cong.fishisland.model.dto.comment.CommentQueryRequest;
import com.cong.fishisland.model.entity.comment.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.vo.comment.CommentNodeVO;
import com.cong.fishisland.model.vo.comment.CommentVO;

/**
* @author 许林涛
* @description 针对表【comment(评论表)】的数据库操作Service
* @createDate 2025-07-03 15:57:04
*/
public interface CommentService extends IService<Comment> {
    /**
     * 添加评论
     */
    Long addComment(Comment comment);

    /**
     * 获取评论树
     */
    Page<CommentNodeVO> getCommentTreeByPostId(CommentQueryRequest commentQueryRequest);

    /**
     * 获取二级评论
     */
    Page<CommentVO> getChildComments(ChildCommentQueryRequest request);

    /**
     * 获取评论数
     */
    Integer getCommentNum(Long postId);

    /**
     * 获取帖子最新一条评论
     */
    CommentVO getLatestComment(Long postId);

    /**
     * 获取帖子点赞最高一条评论
     */
    CommentVO getThumbComment(Long postId);
}
