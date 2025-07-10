package com.cong.fishisland.service.impl.comment;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.CommonConstant;
import com.cong.fishisland.mapper.comment.CommentMapper;
import com.cong.fishisland.mapper.comment.CommentThumbMapper;
import com.cong.fishisland.model.dto.comment.ChildCommentQueryRequest;
import com.cong.fishisland.model.dto.comment.CommentQueryRequest;
import com.cong.fishisland.model.entity.comment.Comment;
import com.cong.fishisland.model.entity.comment.CommentThumb;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.comment.CommentNodeVO;
import com.cong.fishisland.model.vo.comment.CommentVO;
import com.cong.fishisland.service.CommentService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.utils.SqlUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import toolgood.words.StringSearch;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 许林涛
 * @description 针对表【comment(评论表)】的数据库操作Service实现
 * @createDate 2025-07-03 15:57:04
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment>
        implements CommentService {

    @Resource
    private StringSearch wordsUtil;

    @Resource
    private UserService userService;

    @Resource
    private CommentThumbMapper commentThumbMapper;

    @Override
    public Long addComment(Comment comment) {
        // 参数校验
        validComment(comment);
        if (!this.save(comment)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "评论添加失败");
        }
        return comment.getId();
    }

    @Override
    public Page<CommentNodeVO> getCommentTreeByPostId(CommentQueryRequest commentQueryRequest) {
        // 参数校验
        validCommentQueryRequest(commentQueryRequest);
        Long postId = commentQueryRequest.getPostId();
        int current = commentQueryRequest.getCurrent();
        int size = commentQueryRequest.getPageSize();
        String sortField = commentQueryRequest.getSortField();
        String sortOrder = commentQueryRequest.getSortOrder();
        // 获取评论总数
        long totalComments = this.count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
        );
        // 分页查询顶级评论
        Page<Comment> topPage = new Page<>(current, size);
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("postId", postId);
        queryWrapper.isNull("parentId");
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        List<Comment> topComments = this.page(topPage, queryWrapper).getRecords();
        if (CollUtil.isEmpty(topComments)) {
            return new Page<>(current, size, totalComments);
        }
        // 收集所有需要查询的用户ID
        Set<Long> userIds = new HashSet<>();
        topComments.forEach(comment -> userIds.add(comment.getUserId()));
        // 为每个顶级评论加载部分二级评论
        Map<Long, List<Comment>> childrenMap = new HashMap<>();
        for (Comment top : topComments) {
            // 加载前3条二级评论
            List<Comment> children = this.list(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getRootId, top.getId())
                    .eq(Comment::getIsDelete, 0)
                    .orderByAsc(Comment::getCreateTime)
                    .last("LIMIT 3")
            );
            childrenMap.put(top.getId(), children);
            children.forEach(child -> userIds.add(child.getUserId()));
        }

        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            users.forEach(user -> userMap.put(user.getId(), user));
        }

        // 构建节点列表
        List<CommentNodeVO> nodes = topComments.stream().map(top -> {
            CommentNodeVO node = new CommentNodeVO();
            BeanUtils.copyProperties(top, node);

            // 填充顶级评论用户信息
            User user = userMap.get(top.getUserId());
            if (user != null) {
                node.setUser(userService.getUserVO(user));
            }

            // 获取二级评论总数
            node.setChildCount((int) this.count(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, top.getId())
            ));

            // 填充二级评论用户信息
            List<Comment> children = childrenMap.get(top.getId());
            List<CommentVO> childVOs = children.stream().map(child -> {
                CommentVO vo = new CommentVO();
                BeanUtils.copyProperties(child, vo);

                User childUser = userMap.get(child.getUserId());
                if (childUser != null) {
                    vo.setUser(userService.getUserVO(childUser));
                }
                vo.setHasThumb(hasCommentThumb(child.getId()));
                return vo;
            }).collect(Collectors.toList());
            node.setPreviewChildren(childVOs);
            node.setHasThumb(hasCommentThumb(top.getId()));
            return node;
        }).collect(Collectors.toList());
        Page<CommentNodeVO> pageResult = new Page<>();
        if (CollUtil.isNotEmpty(nodes)) {
            pageResult.setCurrent(current);
            pageResult.setSize(size);
            pageResult.setTotal(totalComments);
            pageResult.setRecords(nodes);
        }
        return pageResult;
    }

    @Override
    public Page<CommentVO> getChildComments(ChildCommentQueryRequest request) {
        // 参数校验
        validChildCommentQueryRequest(request);
        Long rootId = request.getRootId();
        int current = request.getCurrent();
        int size = request.getPageSize();

        // 分页查询二级评论
        Page<Comment> pageInfo = new Page<>(current, size);
        List<Comment> children = this.page(pageInfo, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, rootId)
                .eq(Comment::getIsDelete, 0)
                .orderByAsc(Comment::getCreateTime)
        ).getRecords();

        // 处理空结果
        if (CollUtil.isEmpty(children)) {
            return new Page<>(current, size, pageInfo.getTotal());
        }

        // 收集用户ID并批量查询
        Set<Long> userIds = children.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 转换为VO并填充用户信息
        List<CommentVO> commentVOs = children.stream().map(child -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(child, vo);
            User user = userMap.get(child.getUserId());
            if (user != null) {
                vo.setUser(userService.getUserVO(user));
            }
            vo.setHasThumb(hasCommentThumb(child.getId()));
            return vo;
        }).collect(Collectors.toList());

        // 构建返回结果
        Page<CommentVO> pageResult = new Page<>(current, size, pageInfo.getTotal());
        pageResult.setRecords(commentVOs);
        return pageResult;
    }

    @Override
    public Integer getCommentNum(Long postId) {
        ThrowUtils.throwIf(postId == null, ErrorCode.PARAMS_ERROR, "帖子id不能为空");
        return (int) this.count(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
        );
    }

    @Override
    public CommentVO getLatestComment(Long postId) {
        ThrowUtils.throwIf(postId == null, ErrorCode.PARAMS_ERROR, "帖子id不能为空");
        Comment comment = this.getOne(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByDesc(Comment::getCreateTime)
                .last("LIMIT 1")
        );
        return comment == null ? null : safeGetCommentVO(comment);
    }

    @Override
    public CommentVO getThumbComment(Long postId) {
        ThrowUtils.throwIf(postId == null, ErrorCode.PARAMS_ERROR, "帖子id不能为空");
        Comment comment = this.getOne(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getThumbNum)
                .last("LIMIT 1")
        );
        return comment == null ? null : safeGetCommentVO(comment);
    }

    private CommentVO safeGetCommentVO(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            commentVO.setUser(userService.getUserVO(user));
        }
        return commentVO;
    }

    private void validChildCommentQueryRequest(ChildCommentQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(request.getRootId() == null, ErrorCode.PARAMS_ERROR, "根评论id不能为空");
        // 限制爬虫
        ThrowUtils.throwIf(request.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
    }

    private void validCommentQueryRequest(CommentQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(request.getPostId() == null, ErrorCode.PARAMS_ERROR, "帖子id不能为空");
        // 限制爬虫
        ThrowUtils.throwIf(request.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
    }

    private void validComment(Comment comment) {
        ThrowUtils.throwIf(comment == null, ErrorCode.PARAMS_ERROR, "参数为空");
        String content = comment.getContent();
        ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");
        ThrowUtils.throwIf(comment.getPostId() == null, ErrorCode.PARAMS_ERROR, "帖子id不能为空");
        // 敏感词校验（标题和内容）
        String contentSensitiveWord = wordsUtil.FindFirst(content);
        // 构建敏感词提示信息
        ThrowUtils.throwIf(StringUtils.isNotBlank(contentSensitiveWord), ErrorCode.PARAMS_ERROR, "内容包含敏感词: " + contentSensitiveWord);
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
    }

    private Boolean hasCommentThumb(Long commentId) {
        User loginUser = userService.getLoginUserPermitNull();
        if (commentId == null || loginUser == null) {
            return false;
        }
        return commentThumbMapper.selectOne(new LambdaQueryWrapper<CommentThumb>()
                .select(CommentThumb::getId)
                .eq(CommentThumb::getCommentId, commentId)
                .eq(CommentThumb::getUserId, loginUser.getId())
                .last("LIMIT 1")
        ) != null;
    }

}




