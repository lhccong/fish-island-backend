package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.EmoticonFavourService;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 群聊机器人消息广播与持久化
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RobotChatMessageService {
    private static final int BIG_FISH_EMOTICON_PERCENT = 30;

    private final WebSocketService webSocketService;
    private final RoomMessageService roomMessageService;
    private final EmoticonFavourService emoticons;

    public MessageWrapper buildAiReplyWrapper(String answer, Message quotedMessage) {
        return buildMessageWrapper(answer, quotedMessage);
    }

    public void saveAiReply(MessageWrapper messageWrapper) {
        saveReply(messageWrapper, Long.parseLong(UserConstant.ROBOT_ID));
    }

    public Message saveBigFishReply(String text) {
        return saveBigFishReply(text, ThreadLocalRandom.current().nextInt(100) < BIG_FISH_EMOTICON_PERCENT);
    }

    Message saveBigFishReply(String text, boolean includeEmoticon) {
        Message message = new Message();
        message.setId(IdWorker.getIdStr());
        message.setRoomId("-1");
        message.setTimestamp(Instant.now().toString());
        message.setSender(Sender.builder().id(UserConstant.BIG_FISH_ID).name("大肥鱼").build());
        message.setBigFish(true);
        message.setContent(text);
        if (includeEmoticon) message.setBigFishEmoticonUrl(pickBigFishEmoticon());
        MessageWrapper wrapper = new MessageWrapper();
        wrapper.setMessage(message);
        if (!saveReply(wrapper, Long.parseLong(UserConstant.BIG_FISH_ID))) {
            throw new IllegalStateException("Big fish reply was not saved");
        }
        return message;
    }

    private String pickBigFishEmoticon() {
        try {
            String source = emoticons.pickRandomSourceForUser(1L);
            if (source == null || source.length() > 8192) return null;
            URI uri = URI.create(source.trim());
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                return null;
            }
            return uri.toASCIIString();
        } catch (RuntimeException failure) {
            log.warn("Big fish emoticon unavailable: {}", failure.getClass().getSimpleName());
            return null;
        }
    }

    private boolean saveReply(MessageWrapper messageWrapper, long senderId) {
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(senderId);
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(messageWrapper));
        roomMessage.setMessageId(messageWrapper.getMessage().getId());
        return roomMessageService.save(roomMessage);
    }

    public void sendAndSaveAiReply(String answer, Message quotedMessage) {
        MessageWrapper messageWrapper = buildAiReplyWrapper(answer, quotedMessage);

        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(messageWrapper).build());

        saveAiReply(messageWrapper);
    }

    private static @NotNull MessageWrapper buildMessageWrapper(String answer, Message quotedMessage) {
        Message aiMessage = new Message();
        aiMessage.setContent(answer);
        aiMessage.setId(String.valueOf(System.currentTimeMillis()));
        Sender aiSender = Sender.builder()
                .id("-1")
                .level(1)
                .name("摸鱼助手")
                .isAdmin(false)
                .points(-999)
                .avatar("https://oss.cqbo.com/moyu/user_avatar/1/hYskW0jH-34eaba5c-3809-45ef-a3bd-dd01cf97881b_478ce06b6d869a5a11148cf3ee119bac.gif")
                .build();
        aiMessage.setSender(aiSender);
        aiMessage.setTimestamp(String.valueOf(System.currentTimeMillis()));
        aiMessage.setQuotedMessage(quotedMessage);
        if (quotedMessage != null && quotedMessage.getSender() != null) {
            aiMessage.setMentionedUsers(Collections.singletonList(quotedMessage.getSender()));
        }

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(aiMessage);
        return messageWrapper;
    }
}
