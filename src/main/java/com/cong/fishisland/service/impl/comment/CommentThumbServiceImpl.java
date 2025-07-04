package com.cong.fishisland.service.impl.comment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.mapper.comment.CommentMapper;
import com.cong.fishisland.model.entity.comment.Comment;
import com.cong.fishisland.model.entity.comment.CommentThumb;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.service.CommentThumbService;
import com.cong.fishisland.mapper.comment.CommentThumbMapper;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
* @author 许林涛
* @description 针对表【comment_thumb(评论点赞表)】的数据库操作Service实现
* @createDate 2025-07-03 15:57:04
*/
@Service
public class CommentThumbServiceImpl extends ServiceImpl<CommentThumbMapper, CommentThumb>
        implements CommentThumbService {

    @Resource
    private CommentMapper commentMapper;

    @Override
    public int doCommentThumb(long commentId, User loginUser) {
        // 判断评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }

        // 每个用户串行操作
        long userId = loginUser.getId();
        CommentThumbService thumbService = (CommentThumbService) AopContext.currentProxy();
        synchronized (String.valueOf(userId).intern()) {
            return thumbService.doCommentThumbInner(userId, commentId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doCommentThumbInner(long userId, long commentId) {
        CommentThumb commentThumb = new CommentThumb();
        commentThumb.setUserId(userId);
        commentThumb.setCommentId(commentId);

        QueryWrapper<CommentThumb> thumbQueryWrapper = new QueryWrapper<>(commentThumb);
        CommentThumb oldThumb = this.getOne(thumbQueryWrapper);

        boolean result;
        // 已点赞
        if (oldThumb != null) {
            result = this.remove(thumbQueryWrapper);
            if (result) {
                // 点赞数 -1
                result = commentMapper.update(null,
                        new UpdateWrapper<Comment>()
                                .eq("id", commentId)
                                .gt("thumbNum", 0)
                                .setSql("thumbNum = thumbNum - 1")
                ) > 0;
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消点赞失败");
            }
        } else {
            // 未点赞
            result = this.save(commentThumb);
            if (result) {
                // 点赞数 +1
                result = commentMapper.update(null,
                        new UpdateWrapper<Comment>()
                                .eq("id", commentId)
                                .setSql("thumbNum = thumbNum + 1")
                ) > 0;
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞失败");
            }
        }
    }
}





