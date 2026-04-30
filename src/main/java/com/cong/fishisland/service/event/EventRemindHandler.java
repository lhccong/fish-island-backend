package com.cong.fishisland.service.event;

import com.cong.fishisland.constant.ActionTypeConstant;
import com.cong.fishisland.constant.SourceTypeConstant;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.service.EventRemindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 事件提醒处理服务
 *
 * @author 许林涛
 * @date 2025年07月09日 15:32
 */
@Slf4j
@Service
public class EventRemindHandler {
    @Resource
    private EventRemindService eventRemindService;

    /**
     * 异步处理点赞动态事件
     */
    @Async("eventRemindExecutor")
    public void handlePostLike(Long postId, Long senderId, Long recipientId) {
        // 检查是否已存在相同事件
        if (eventRemindService.existsEvent(
                ActionTypeConstant.LIKE,
                postId,
                SourceTypeConstant.POST,
                senderId,
                recipientId)) {
            log.info("已存在点赞动态事件，跳过保存: postId={}, senderId={}", postId, senderId);
            return;
        }

        // 创建事件提醒
        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.LIKE);
        event.setSourceId(postId);
        event.setSourceType(SourceTypeConstant.POST);
        event.setSourceContent("用户点赞了你的动态");
        event.setUrl(String.valueOf(postId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
        log.info("保存点赞动态事件: postId={}, senderId={}, recipientId={}",
                postId, senderId, recipientId);
    }

    /**
     * 异步处理点赞评论事件
     */
    @Async("eventRemindExecutor")
    public void handleCommentLike(Long commentId, Long senderId, Long recipientId, Long postId) {
        // 检查是否已存在相同事件
        if (eventRemindService.existsEvent(
                ActionTypeConstant.LIKE,
                commentId,
                SourceTypeConstant.COMMENT,
                senderId,
                recipientId)) {
            log.info("已存在点赞评论事件，跳过保存: commentId={}, senderId={}", commentId, senderId);
            return;
        }

        // 创建事件提醒
        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.LIKE);
        event.setSourceId(commentId);
        event.setSourceType(SourceTypeConstant.COMMENT);
        event.setSourceContent("用户点赞了你的评论");
        event.setUrl(String.valueOf(postId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());

        eventRemindService.save(event);
        log.info("保存点赞评论事件: commentId={}, senderId={}, recipientId={}",
                commentId, senderId, recipientId);
    }

    /**
     * 异步处理评论事件
     */
    @Async("eventRemindExecutor")
    public void handleComment(Long commentId, Long postId,
                              Long senderId, Long recipientId,
                              String content, Boolean isReply) {
        // 创建事件提醒
        EventRemind event = new EventRemind();
        if (isReply) {
            event.setAction(ActionTypeConstant.REPLY);
        } else {
            event.setAction(ActionTypeConstant.COMMENT);
        }
        event.setSourceId(commentId);
        event.setSourceType(SourceTypeConstant.COMMENT);
        event.setSourceContent(content);
        event.setUrl(String.valueOf(postId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());

        eventRemindService.save(event);
        log.info("保存评论事件: commentId={}, postId={}, senderId={}, recipientId={}",
                commentId, postId, senderId, recipientId);
    }


    /**
     * 异步处理朋友圈点赞事件
     */
    @Async("eventRemindExecutor")
    public void handleMomentsLike(Long momentId, Long senderId, Long recipientId) {
        if (eventRemindService.existsEvent(
                ActionTypeConstant.LIKE,
                momentId,
                SourceTypeConstant.MOMENTS,
                senderId,
                recipientId)) {
            log.info("已存在点赞朋友圈事件，跳过保存: momentId={}, senderId={}", momentId, senderId);
            return;
        }
        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.LIKE);
        event.setSourceId(momentId);
        event.setSourceType(SourceTypeConstant.MOMENTS);
        event.setSourceContent("用户点赞了你的朋友圈");
        event.setUrl(String.valueOf(momentId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
        log.info("保存点赞朋友圈事件: momentId={}, senderId={}, recipientId={}", momentId, senderId, recipientId);
    }

    /**
     * 异步处理朋友圈评论事件
     */
    @Async("eventRemindExecutor")
    public void handleMomentsComment(Long commentId, Long momentId,
                                     Long senderId, Long recipientId,
                                     String content, Boolean isReply) {
        EventRemind event = new EventRemind();
        event.setAction(isReply ? ActionTypeConstant.REPLY : ActionTypeConstant.COMMENT);
        event.setSourceId(momentId);
        event.setSourceType(SourceTypeConstant.MOMENTS);
        event.setSourceContent(content);
        event.setUrl(String.valueOf(momentId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
        log.info("保存朋友圈评论事件: commentId={}, momentId={}, senderId={}, recipientId={}", commentId, momentId, senderId, recipientId);
    }

    /**
     * 异步处理朋友圈打赏事件
     */
    @Async("eventRemindExecutor")
    public void handleMomentsReward(Long momentId, Long senderId, Long recipientId, Integer points) {
        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.SYSTEM);
        event.setSourceId(momentId);
        event.setSourceType(SourceTypeConstant.SYSTEM);
        event.setSourceContent("用户打赏了你 " + points + " 积分");
        event.setUrl(String.valueOf(momentId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
        log.info("保存打赏朋友圈事件: momentId={}, senderId={}, recipientId={}, points={}", momentId, senderId, recipientId, points);
    }

    /**
     * 异步处理关注事件
     */
    @Async("eventRemindExecutor")
    public void handleSystemMessage(Long recipientId, String content) {

        // 创建事件提醒
        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.SYSTEM);
        event.setSourceType(SourceTypeConstant.SYSTEM);
        event.setSourceContent(content);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());
        event.setUrl("");
        event.setSourceId(-1L);
        event.setSenderId(-1L);
        eventRemindService.save(event);

        log.info("保存系统消息事件: recipientId={}", recipientId);

    }

}
