package com.cong.fishisland.service.impl.moments;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.moments.MomentsCommentMapper;
import com.cong.fishisland.mapper.moments.MomentsLikeMapper;
import com.cong.fishisland.mapper.moments.MomentsMapper;
import com.cong.fishisland.model.dto.moments.MomentsAddRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentAddRequest;
import com.cong.fishisland.model.dto.moments.MomentsCommentQueryRequest;
import com.cong.fishisland.model.dto.moments.MomentsQueryRequest;
import com.cong.fishisland.model.entity.moments.Moments;
import com.cong.fishisland.model.entity.moments.MomentsComment;
import com.cong.fishisland.model.entity.moments.MomentsLike;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.moments.MomentsCommentVO;
import com.cong.fishisland.model.vo.moments.MomentsVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.event.EventRemindHandler;
import com.cong.fishisland.service.moments.MomentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 朋友圈服务实现
 *
 * @author cong
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MomentsServiceImpl extends ServiceImpl<MomentsMapper, Moments>
        implements MomentsService {

    private final MomentsLikeMapper momentsLikeMapper;
    private final MomentsCommentMapper momentsCommentMapper;
    private final UserService userService;
    private final EventRemindHandler eventRemindHandler;

    @Override
    public Long publishMoment(MomentsAddRequest request) {
        ThrowUtils.throwIf(
                !StringUtils.hasText(request.getContent()) &&
                        (request.getMediaJson() == null || request.getMediaJson().isEmpty()),
                ErrorCode.PARAMS_ERROR, "内容和媒体资源不能同时为空"
        );

        long userId = StpUtil.getLoginIdAsLong();
        Moments moments = new Moments();
        BeanUtils.copyProperties(request, moments);
        moments.setUserId(userId);
        moments.setLikeNum(0);
        moments.setCommentNum(0);
        if (moments.getVisibility() == null) {
            moments.setVisibility(0);
        }
        save(moments);
        return moments.getId();
    }

    @Override
    public void deleteMoment(Long momentId) {
        long userId = StpUtil.getLoginIdAsLong();
        Moments moments = getById(momentId);
        ThrowUtils.throwIf(moments == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!moments.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        removeById(momentId);
    }

    @Override
    public Page<MomentsVO> listMoments(MomentsQueryRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        Page<Moments> page = new Page<>(request.getCurrent(), request.getPageSize());

        lambdaQuery()
                .eq(request.getUserId() != null, Moments::getUserId, request.getUserId())
                // 过滤仅自己可见（visibility=1）的其他人动态
                .and(w -> w.eq(Moments::getUserId, userId)
                        .or().ne(Moments::getVisibility, 1))
                .orderByDesc(Moments::getCreateTime)
                .page(page);

        // 批量查询点赞状态
        List<Long> momentIds = page.getRecords().stream()
                .map(Moments::getId).collect(Collectors.toList());
        Set<Long> likedSet = getLikedSet(userId, momentIds);

        // 批量查询用户信息
        Set<Long> userIds = page.getRecords().stream()
                .map(Moments::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = getUserMap(userIds);

        List<MomentsVO> voList = page.getRecords().stream()
                .map(m -> toVO(m, likedSet, userMap))
                .collect(Collectors.toList());

        Page<MomentsVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long momentId) {
        long userId = StpUtil.getLoginIdAsLong();
        Moments moments = getById(momentId);
        ThrowUtils.throwIf(moments == null, ErrorCode.NOT_FOUND_ERROR);

        MomentsLike existing = momentsLikeMapper.selectOne(
                new LambdaQueryWrapper<MomentsLike>()
                        .eq(MomentsLike::getMomentId, momentId)
                        .eq(MomentsLike::getUserId, userId));

        if (existing != null) {
            // 取消点赞
            momentsLikeMapper.deleteById(existing.getId());
            lambdaUpdate()
                    .eq(Moments::getId, momentId)
                    .setSql("likeNum = GREATEST(likeNum - 1, 0)")
                    .update();
            return false;
        } else {
            // 点赞
            MomentsLike like = new MomentsLike();
            like.setMomentId(momentId);
            like.setUserId(userId);
            momentsLikeMapper.insert(like);
            lambdaUpdate()
                    .eq(Moments::getId, momentId)
                    .setSql("likeNum = likeNum + 1")
                    .update();
            // 异步通知（避免通知自己）
            if (!moments.getUserId().equals(userId)) {
                eventRemindHandler.handleMomentsLike(momentId, userId, moments.getUserId());
            }
            return true;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(MomentsCommentAddRequest request) {
        ThrowUtils.throwIf(!StringUtils.hasText(request.getContent()), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(request.getMomentId() == null, ErrorCode.PARAMS_ERROR, "动态ID不能为空");

        Moments moments = getById(request.getMomentId());
        ThrowUtils.throwIf(moments == null, ErrorCode.NOT_FOUND_ERROR);

        long userId = StpUtil.getLoginIdAsLong();
        MomentsComment comment = new MomentsComment();
        BeanUtils.copyProperties(request, comment);
        comment.setUserId(userId);
        momentsCommentMapper.insert(comment);

        lambdaUpdate()
                .eq(Moments::getId, request.getMomentId())
                .setSql("commentNum = commentNum + 1")
                .update();

        // 异步通知
        Long recipientId = null;
        boolean isReply = false;
        if (request.getParentId() != null) {
            // 回复评论，通知被回复的用户
            MomentsComment parentComment = momentsCommentMapper.selectById(request.getParentId());
            if (parentComment != null && !parentComment.getUserId().equals(userId)) {
                recipientId = parentComment.getUserId();
                isReply = true;
            }
        } else {
            // 直接评论动态，通知动态作者
            if (!moments.getUserId().equals(userId)) {
                recipientId = moments.getUserId();
            }
        }
        if (recipientId != null) {
            eventRemindHandler.handleMomentsComment(comment.getId(), request.getMomentId(),
                    userId, recipientId, request.getContent(), isReply);
        }

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        long userId = StpUtil.getLoginIdAsLong();
        MomentsComment comment = momentsCommentMapper.selectById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!comment.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);

        momentsCommentMapper.deleteById(commentId);
        lambdaUpdate()
                .eq(Moments::getId, comment.getMomentId())
                .setSql("commentNum = GREATEST(commentNum - 1, 0)")
                .update();
    }

    @Override
    public Page<MomentsCommentVO> listComments(MomentsCommentQueryRequest request) {
        ThrowUtils.throwIf(request.getMomentId() == null, ErrorCode.PARAMS_ERROR, "动态ID不能为空");

        // 分页查询顶级评论
        Page<MomentsComment> page = new Page<>(request.getCurrent(), request.getPageSize());
        momentsCommentMapper.selectPage(page,
                new LambdaQueryWrapper<MomentsComment>()
                        .eq(MomentsComment::getMomentId, request.getMomentId())
                        .isNull(MomentsComment::getParentId)
                        .orderByAsc(MomentsComment::getCreateTime));

        if (page.getRecords().isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 查询这批顶级评论下的所有子评论
        List<Long> parentIds = page.getRecords().stream()
                .map(MomentsComment::getId).collect(Collectors.toList());
        List<MomentsComment> children = momentsCommentMapper.selectList(
                new LambdaQueryWrapper<MomentsComment>()
                        .eq(MomentsComment::getMomentId, request.getMomentId())
                        .in(MomentsComment::getParentId, parentIds)
                        .orderByAsc(MomentsComment::getCreateTime));

        // 批量查询用户信息
        Set<Long> userIds = new HashSet<>();
        page.getRecords().forEach(c -> userIds.add(c.getUserId()));
        children.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyUserId() != null) {
                userIds.add(c.getReplyUserId());
            }
        });
        Map<Long, User> userMap = getUserMap(userIds);

        // 子评论按 parentId 分组
        Map<Long, List<MomentsCommentVO>> childrenMap = children.stream()
                .map(c -> toCommentVO(c, userMap))
                .collect(Collectors.groupingBy(MomentsCommentVO::getParentId));

        // 组装顶级评论 + 子评论
        List<MomentsCommentVO> voList = page.getRecords().stream().map(c -> {
            MomentsCommentVO vo = toCommentVO(c, userMap);
            vo.setChildren(childrenMap.getOrDefault(c.getId(), Collections.emptyList()));
            return vo;
        }).collect(Collectors.toList());

        Page<MomentsCommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ---- helpers ----

    private MomentsVO toVO(Moments m, Set<Long> likedSet, Map<Long, User> userMap) {
        MomentsVO vo = new MomentsVO();
        BeanUtils.copyProperties(m, vo);
        vo.setLiked(likedSet.contains(m.getId()));
        User user = userMap.get(m.getUserId());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
        }
        return vo;
    }

    private MomentsCommentVO toCommentVO(MomentsComment c, Map<Long, User> userMap) {
        MomentsCommentVO vo = new MomentsCommentVO();
        BeanUtils.copyProperties(c, vo);
        User user = userMap.get(c.getUserId());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setUserAvatar(user.getUserAvatar());
        }
        if (c.getReplyUserId() != null) {
            User replyUser = userMap.get(c.getReplyUserId());
            if (replyUser != null) {
                vo.setReplyUserName(replyUser.getUserName());
            }
        }
        return vo;
    }

    private Set<Long> getLikedSet(long userId, List<Long> momentIds) {
        if (momentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return momentsLikeMapper.selectList(
                new LambdaQueryWrapper<MomentsLike>()
                        .eq(MomentsLike::getUserId, userId)
                        .in(MomentsLike::getMomentId, momentIds))
                .stream()
                .map(MomentsLike::getMomentId)
                .collect(Collectors.toSet());
    }

    private Map<Long, User> getUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }
}
