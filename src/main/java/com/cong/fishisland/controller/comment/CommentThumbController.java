package com.cong.fishisland.controller.comment;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.dto.commentthumb.CommentThumbAddRequest;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.CommentThumbService;
import com.cong.fishisland.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 许林涛
 * @date 2025年07月04日 17:15
 */
@RestController
@RequestMapping("/comment_thumb")
@Slf4j
public class CommentThumbController {

    @Resource
    private CommentThumbService commentThumbService;

    @Resource
    private UserService userService;

    /**
     * 点赞 / 取消点赞
     *
     * @param request 评论点赞请求
     * @return resultNum 本次点赞变化数
     */
    @PostMapping("/")
    @ApiOperation(value = "评论点赞/取消点赞")
    public BaseResponse<Integer> doThumb(@RequestBody CommentThumbAddRequest request) {
        if (request == null || request.getCommentId() == null || request.getCommentId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能点赞
        User loginUser = userService.getLoginUser();
        long commentId = request.getCommentId();
        int result = commentThumbService.doCommentThumb(commentId, loginUser);
        return ResultUtils.success(result);
    }
}
