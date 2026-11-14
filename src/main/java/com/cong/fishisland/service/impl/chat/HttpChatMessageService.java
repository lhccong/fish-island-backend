package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.chat.SendMessageRequest;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.vo.IpLocationVO;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.*;
import com.cong.fishisland.websocket.event.AddSpeakPointEvent;
import com.cong.fishisland.websocket.event.AIAnswerEvent;
import com.cong.fishisland.websocket.service.WebSocketService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import toolgood.words.StringSearch;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** HTTP command path. Existing WebSocket transport is used only for fan-out. */
@Service
@RequiredArgsConstructor
@Slf4j
public class HttpChatMessageService {
    private final UserService userService;
    private final UserPointsService userPointsService;
    private final RoomMessageService roomMessageService;
    private final UserMuteService userMuteService;
    private final UserVipService userVipService;
    private final IpLocationService ipLocationService;
    private final WebSocketService webSocketService;
    private final ApplicationEventPublisher publisher;
    private final StringSearch wordsUtil;
    private final RedissonClient redisson;
    private final Cache<String, IpLocationVO> locations = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).build();

    public MessageWrapper send(SendMessageRequest input, String ip) {
        User user = userService.getLoginUser();
        if (UserConstant.BAN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被封禁");
        }
        validate(input);
        String key = "chat:http-send:" + user.getId() + ":" + input.getClientMessageId();
        RLock lock = redisson.getLock(key + ":lock");
        lock.lock();
        try {
            RBucket<String> reservation = redisson.getBucket(key + ":id");
            String messageId = reservation.get();
            RBucket<String> receipt = redisson.getBucket(key + ":receipt");
            if (messageId != null) {
                RoomMessage existing = find(messageId);
                if (existing != null) {
                    MessageWrapper wrapper = JSON.parseObject(existing.getMessageJson(), MessageWrapper.class);
                    Message message = wrapper.getMessage();
                    String normalized = wordsUtil.Replace(input.getContent()).replace("```", "");
                    String quotedId = message.getQuotedMessage() == null ? null : message.getQuotedMessage().getId();
                    if (!normalized.equals(message.getContent()) || !java.util.Objects.equals(input.getQuotedMessageId(), quotedId)) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复发送标识不能用于不同消息");
                    }
                    broadcast(wrapper);
                    return wrapper;
                }
                if (receipt.isExists()) throw new BusinessException(ErrorCode.PARAMS_ERROR, "该消息已撤回，不能重新发送");
            }
            if (Boolean.TRUE.equals(userMuteService.getUserMuteInfo(user.getId()).getIsMuted())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "你已被禁言，暂时无法发送消息");
            }
            Message message = new Message();
            message.setContent(wordsUtil.Replace(input.getContent()).replace("```", ""));
            if (!StringUtils.hasText(message.getContent())) throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
            message.setClientMessageId(input.getClientMessageId());
            message.setRoomId("-1");
            message.setTimestamp(Instant.now().toString());
            message.setSender(sender(user, ip));
            if (input.getQuotedMessageId() != null) {
                RoomMessage original = find(input.getQuotedMessageId());
                if (original == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "引用消息不存在或已撤回");
                Message quoted = JSON.parseObject(original.getMessageJson(), MessageWrapper.class).getMessage();
                quoted.setQuotedMessage(null);
                quoted.setMentionedUsers(null);
                message.setQuotedMessage(quoted);
            }
            if (messageId == null) {
                messageId = nextMessageId();
                reservation.set(messageId, 7, TimeUnit.DAYS);
            }
            message.setId(messageId);
            MessageWrapper wrapper = new MessageWrapper();
            wrapper.setMessage(message);
            RoomMessage record = new RoomMessage();
            record.setRoomId(-1L);
            record.setUserId(user.getId());
            record.setMessageId(messageId);
            record.setMessageJson(JSON.toJSONString(wrapper));
            if (!roomMessageService.save(record)) throw new BusinessException(ErrorCode.OPERATION_ERROR, "消息保存失败，请重试");
            receipt.set("saved", 7, TimeUnit.DAYS);
            broadcast(wrapper);
            try {
                publisher.publishEvent(new AddSpeakPointEvent(this, user.getId().toString()));
                if (message.getContent().contains("@摸鱼助手")) publisher.publishEvent(new AIAnswerEvent(this, wrapper));
            } catch (RuntimeException ignored) {
                log.warn("Chat side effect failed after message persistence");
            }
            return wrapper;
        } finally {
            lock.unlock();
        }
    }

    private RoomMessage find(String id) {
        return roomMessageService.getOne(new LambdaQueryWrapper<RoomMessage>()
                .eq(RoomMessage::getRoomId, -1L).eq(RoomMessage::getMessageId, id));
    }

    private void broadcast(MessageWrapper wrapper) {
        // No skipUid: the sender and every tab/device receive the canonical message too.
        webSocketService.sendToAllOnline(WSBaseResp.builder().type(MessageTypeEnum.CHAT.getType()).data(wrapper).build());
    }

    private String nextMessageId() {
        RAtomicLong sequence = redisson.getAtomicLong("chat:http-send:sequence");
        while (true) {
            long previous = sequence.get();
            long next = Math.max(System.currentTimeMillis(), previous + 1);
            if (sequence.compareAndSet(previous, next)) return Long.toString(next);
        }
    }

    private void validate(SendMessageRequest input) {
        if (input == null || !StringUtils.hasText(input.getContent()) || input.getContent().length() > 5000
                || input.getClientMessageId() == null || !input.getClientMessageId().matches("[a-zA-Z0-9-]{16,64}")
                || (input.getQuotedMessageId() != null && !input.getQuotedMessageId().matches("[0-9]{1,20}"))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息参数不合法（正文最多 5000 字）");
        }
    }

    private Sender sender(User user, String ip) {
        UserPoints points = userPointsService.getOne(new LambdaQueryWrapper<UserPoints>()
                .select(UserPoints::getPoints, UserPoints::getLevel)
                .eq(UserPoints::getUserId, user.getId()));
        Sender sender = new Sender();
        sender.setId(user.getId().toString());
        sender.setName(user.getUserName());
        sender.setAvatar(user.getUserAvatar());
        sender.setUserProfile(user.getUserProfile());
        sender.setAvatarFramerUrl(user.getAvatarFramerUrl());
        sender.setTitleId(user.getTitleId());
        sender.setTitleIdList(user.getTitleIdList());
        sender.setLevel(points == null || points.getLevel() == null ? 1 : points.getLevel());
        sender.setPoints(points == null || points.getPoints() == null ? 0 : points.getPoints());
        sender.setAdmin(UserConstant.ADMIN_ROLE.equals(user.getUserRole()));
        sender.setVip(userVipService.isUserVip(user.getId()));
        sender.setCountry("未知国家");
        sender.setRegion("未知地区");
        if (StringUtils.hasText(ip)) {
            try {
                IpLocationVO location = locations.get(ip, ipLocationService::getLocationByIp);
                if (location != null) {
                    if (StringUtils.hasText(location.getCountry())) sender.setCountry(location.getCountry());
                    if (StringUtils.hasText(location.getRegionName())) sender.setRegion(location.getRegionName());
                }
            } catch (RuntimeException ignored) {
                // Location is optional metadata and must not reject a valid message.
            }
        }
        return sender;
    }
}
