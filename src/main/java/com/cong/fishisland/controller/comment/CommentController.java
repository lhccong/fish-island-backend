package com.cong.fishisland.controller.comment;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.DeleteRequest;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.dto.comment.ChildCommentQueryRequest;
import com.cong.fishisland.model.dto.comment.CommentAddRequest;
import com.cong.fishisland.model.dto.comment.CommentQueryRequest;
import com.cong.fishisland.model.entity.comment.Comment;
import com.cong.fishisland.model.entity.post.Post;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.comment.CommentNodeVO;
import com.cong.fishisland.model.vo.comment.CommentVO;
import com.cong.fishisland.service.CommentService;
import com.cong.fishisland.service.PostService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.event.EventRemindHandler;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 许林涛
 * @date 2025年07月04日 10:32
 */
@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {
    @Resource
    private CommentService commentService;

    @Resource
    private UserService userService;

    @Resource
    private EventRemindHandler eventRemindHandler;

    @Resource
    private PostService postService;

    /**
     * 添加评论
     *
     * @param request 添加请求
     * @return 添加结果
     */
    @PostMapping("/add")
    @ApiOperation("添加评论")
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest request) {
        User loginUser = userService.getLoginUser();
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setRootId(request.getRootId());
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setUserId(loginUser.getId());
        Long commentId = commentService.addComment(comment);
        // 确定接收者ID（避免通知自己）
        Long recipientId = null;
        Long parentId = comment.getParentId();
        if ( parentId == null) {
            // 如果是顶级评论，通知帖子作者（如果不是自己）
            Post post = postService.getById(comment.getPostId());
            if (post != null && !post.getUserId().equals(comment.getUserId())) {
                recipientId = post.getUserId();
            }
        } else {
            // 如果是回复评论，通知被回复的用户（如果不是自己）
            Comment parentComment = commentService.getById(comment.getParentId());
            if (parentComment != null && !parentComment.getUserId().equals(comment.getUserId())) {
                recipientId = parentComment.getUserId();
            }
        }
        // 异步处理事件提醒
        if (recipientId != null) {
            eventRemindHandler. handleComment(
                    comment.getId(),
                    comment.getPostId(),
                    comment.getUserId(),
                    recipientId,
                    comment.getContent(),
                    parentId != null
            );
        }
        return ResultUtils.success(commentId);
    }

    /**
     * 删除评论
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除评论")
    public BaseResponse<Boolean> deletePost(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || Long.parseLong(deleteRequest.getId()) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser();
        long id = Long.parseLong(deleteRequest.getId());
        // 判断是否存在
        Comment oldComment = commentService.getById(id);
        ThrowUtils.throwIf(oldComment == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldComment.getUserId().equals(user.getId()) && !userService.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = commentService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 获取评论树
     *
     * @param request 获取请求
     * @return 获取结果
     */
    @PostMapping("/list/tree")
    @ApiOperation("获取评论树（分页）")
    public BaseResponse<Page<CommentNodeVO>> getCommentTree(@RequestBody CommentQueryRequest request) {
        return ResultUtils.success(commentService.getCommentTreeByPostId(request));
    }

    /**
     * 获取二级评论
     *
     * @param request 获取请求
     * @return 获取结果
     */
    @PostMapping("/list/children")
    @ApiOperation("获取二级评论（分页）")
    public BaseResponse<Page<CommentVO>> getChildComments(@RequestBody ChildCommentQueryRequest request) {
        return ResultUtils.success(commentService.getChildComments(request));
    }

}
