package com.cong.fishisland.service.event;

import com.cong.fishisland.constant.ActionTypeConstant;
import com.cong.fishisland.constant.SourceTypeConstant;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.service.EventRemindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 仅在新增关注时调用，取消关注不发送
     */
    @Async("eventRemindExecutor")
    public void handleFollow(Long senderId, Long recipientId) {
        // 幂等：同一人重复关注不重复提醒
        if (eventRemindService.existsEvent(
                ActionTypeConstant.FOLLOW,
                recipientId,
                SourceTypeConstant.SYSTEM,
                senderId,
                recipientId)) {
            log.info("已存在关注事件，跳过保存: senderId={}, recipientId={}", senderId, recipientId);
            return;
        }

        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.FOLLOW);
        event.setSourceId(recipientId);
        event.setSourceType(SourceTypeConstant.SYSTEM);
        event.setSourceContent("有人关注了你");
        event.setUrl(String.valueOf(senderId));
        event.setSenderId(senderId);
        event.setRecipientId(recipientId);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
        log.info("保存关注事件: senderId={}, recipientId={}", senderId, recipientId);
    }

    /**
     * 异步处理系统消息事件
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

    /**
     * 异步处理农场被偷菜事件（通知农场主）
     */
    @Async("eventRemindExecutor")
    public void handleFarmSteal(Long stealRecordId, Long landId, Long stealerId, Long ownerId,
                                String cropName, int stealPoints) {
        FarmStealNotifyItem item = new FarmStealNotifyItem(stealRecordId, landId, cropName, stealPoints);
        handleFarmStealBatch(stealerId, ownerId, Collections.singletonList(item));
    }

    /**
     * 异步处理农场被偷菜事件（同一农场主多条记录合并为一条通知）
     */
    @Async("eventRemindExecutor")
    public void handleFarmStealBatch(Long stealerId, Long ownerId, List<FarmStealNotifyItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        int totalPoints = items.stream().mapToInt(FarmStealNotifyItem::getStealPoints).sum();
        int landCount = items.size();

        Set<String> cropLabels = new LinkedHashSet<>();
        for (FarmStealNotifyItem item : items) {
            String cropName = item.getCropName();
            cropLabels.add(cropName != null && !cropName.isEmpty() ? cropName : "作物");
        }

        String sourceContent = buildFarmStealSourceContent(cropLabels, landCount, totalPoints);
        String landIdsUrl = items.stream()
                .map(item -> String.valueOf(item.getLandId()))
                .collect(Collectors.joining(","));

        EventRemind event = new EventRemind();
        event.setAction(ActionTypeConstant.FARM_STEAL);
        event.setSourceId(items.get(0).getStealRecordId());
        event.setSourceType(SourceTypeConstant.FARM);
        event.setSourceContent(sourceContent);
        event.setUrl(landIdsUrl);
        event.setSenderId(stealerId);
        event.setRecipientId(ownerId);
        event.setRemindTime(new Date());
        eventRemindService.save(event);
        log.info("保存农场偷菜事件: stealRecordId={}, landIds={}, stealerId={}, ownerId={}, landCount={}",
                items.get(0).getStealRecordId(), landIdsUrl, stealerId, ownerId, landCount);
    }

    private static String buildFarmStealSourceContent(Set<String> cropLabels, int landCount, int totalPoints) {
        List<String> labels = new ArrayList<>(cropLabels);
        if (labels.size() == 1) {
            String cropPart = "「" + labels.get(0) + "」";
            if (landCount > 1) {
                return "偷走了你的" + cropPart + "等" + landCount + "块地，损失 " + totalPoints + " 积分";
            }
            return "偷走了你的" + cropPart + "，损失 " + totalPoints + " 积分";
        }
        String cropPart = labels.stream().map(name -> "「" + name + "」").collect(Collectors.joining(""));
        return "偷走了你的" + cropPart + "等" + landCount + "块地，损失 " + totalPoints + " 积分";
    }

    /**
     * 农场偷菜通知条目
     */
    public static class FarmStealNotifyItem {
        private final Long stealRecordId;
        private final Long landId;
        private final String cropName;
        private final int stealPoints;

        public FarmStealNotifyItem(Long stealRecordId, Long landId, String cropName, int stealPoints) {
            this.stealRecordId = stealRecordId;
            this.landId = landId;
            this.cropName = cropName;
            this.stealPoints = stealPoints;
        }

        public Long getStealRecordId() {
            return stealRecordId;
        }

        public Long getLandId() {
            return landId;
        }

        public String getCropName() {
            return cropName;
        }

        public int getStealPoints() {
            return stealPoints;
        }
    }

}
