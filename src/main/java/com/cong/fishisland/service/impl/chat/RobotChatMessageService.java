package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 摸鱼助手消息广播与持久化
 */
@Service
@RequiredArgsConstructor
public class RobotChatMessageService {

    private final WebSocketService webSocketService;
    private final RoomMessageService roomMessageService;

    public MessageWrapper buildAiReplyWrapper(String answer, Message quotedMessage) {
        return buildMessageWrapper(answer, quotedMessage);
    }

    public void saveAiReply(MessageWrapper messageWrapper) {
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(-1L);
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(messageWrapper));
        roomMessage.setMessageId(messageWrapper.getMessage().getId());
        roomMessageService.save(roomMessage);
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
