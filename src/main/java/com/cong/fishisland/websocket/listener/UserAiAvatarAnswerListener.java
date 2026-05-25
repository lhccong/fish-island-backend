package com.cong.fishisland.websocket.listener;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.datasource.ai.AIChatDataSource;
import com.cong.fishisland.model.entity.aiavatar.UserAiAvatar;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.vo.user.LoginUserVO;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.service.UserAiAvatarService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserVipService;
import com.cong.fishisland.websocket.event.UserAiAvatarAnswerEvent;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户 AI 分身回答监听器
 */
@Slf4j
@Component
public class UserAiAvatarAnswerListener {

    private static final String AI_MODEL = "Qwen/Qwen2.5-14B-Instruct";

    private final WebSocketService webSocketService;
    private final AIChatDataSource siliconFlowDataSource;
    private final UserAiAvatarService userAiAvatarService;
    private final UserService userService;
    private final UserVipService userVipService;
    private final RoomMessageService roomMessageService;

    public UserAiAvatarAnswerListener(
            WebSocketService webSocketService,
            @Qualifier("siliconFlowDataSource") AIChatDataSource siliconFlowDataSource,
            UserAiAvatarService userAiAvatarService,
            UserService userService,
            UserVipService userVipService,
            RoomMessageService roomMessageService) {
        this.webSocketService = webSocketService;
        this.siliconFlowDataSource = siliconFlowDataSource;
        this.userAiAvatarService = userAiAvatarService;
        this.userService = userService;
        this.userVipService = userVipService;
        this.roomMessageService = roomMessageService;
    }

    @Async
    @EventListener(classes = UserAiAvatarAnswerEvent.class)
    public void sendAnswer(UserAiAvatarAnswerEvent event) {
        MessageWrapper messageDto = event.getMessageDto();
        Message message = messageDto.getMessage();
        Long avatarUserId = event.getAvatarUserId();

        try {
            UserAiAvatar avatar = userAiAvatarService.getEnabledAvatarByUserId(avatarUserId);
            if (avatar == null) {
                return;
            }

            User user = userService.getById(avatarUserId);
            if (user == null) {
                return;
            }

            String content = extractQuestionContent(message, avatarUserId, avatar, user);
            if (CharSequenceUtil.isBlank(content)) {
                return;
            }

            List<SiliconFlowRequest.Message> messages = new ArrayList<>();
            messages.add(new SiliconFlowRequest.Message() {{
                setRole("user");
                setContent(content);
            }});

            List<SiliconFlowRequest.Message> requestMessages = new ArrayList<>(messages);
            requestMessages.add(0, new SiliconFlowRequest.Message() {{
                setRole("system");
                setContent(avatar.getSystemPrompt());
            }});

            AiResponse aiResponse = siliconFlowDataSource.getAiResponse(requestMessages, AI_MODEL);
            if (aiResponse == null || CharSequenceUtil.isBlank(aiResponse.getAnswer())) {
                return;
            }

            sendAndSaveAvatarMessage(aiResponse.getAnswer(), message, user, avatar);
        } catch (Exception e) {
            log.error("用户 AI 分身回复失败, avatarUserId={}", avatarUserId, e);
        }
    }

    private String extractQuestionContent(Message message, Long avatarUserId, UserAiAvatar avatar, User user) {
        String content = message.getContent().trim();
        String avatarUserIdStr = String.valueOf(avatarUserId);

        List<Sender> mentionedUsers = message.getMentionedUsers();
        if (mentionedUsers != null) {
            for (Sender mentioned : mentionedUsers) {
                if (avatarUserIdStr.equals(mentioned.getId()) && CharSequenceUtil.isNotBlank(mentioned.getName())) {
                    content = content.replace("@" + mentioned.getName(), "");
                    break;
                }
            }
        }

        if (CharSequenceUtil.isNotBlank(avatar.getAvatarName())) {
            content = content.replace("@" + avatar.getAvatarName(), "");
        }
        if (CharSequenceUtil.isNotBlank(user.getUserName())) {
            content = content.replace("@" + user.getUserName(), "");
        }
        return content.trim();
    }

    private void sendAndSaveAvatarMessage(String answer, Message message, User user, UserAiAvatar avatar) {
        MessageWrapper messageWrapper = buildAvatarMessageWrapper(answer, message, user, avatar);

        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(messageWrapper)
                .build());

        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(user.getId());
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(messageWrapper));
        roomMessage.setMessageId(messageWrapper.getMessage().getId());
        roomMessageService.save(roomMessage);
    }

    private @NotNull MessageWrapper buildAvatarMessageWrapper(String answer, Message message, User user, UserAiAvatar avatar) {
        LoginUserVO loginUserVO = userService.getLoginUserVO(user);

        Message aiMessage = new Message();
        aiMessage.setContent(answer);
        aiMessage.setId(String.valueOf(System.currentTimeMillis()));
        aiMessage.setSender(buildAvatarSender(user, avatar, loginUserVO));
        aiMessage.setTimestamp(String.valueOf(System.currentTimeMillis()));
        aiMessage.setQuotedMessage(message);
        aiMessage.setMentionedUsers(Collections.singletonList(message.getSender()));

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(aiMessage);
        return messageWrapper;
    }

    private Sender buildAvatarSender(User user, UserAiAvatar avatar, LoginUserVO loginUserVO) {
        int level = loginUserVO != null && loginUserVO.getLevel() != null ? loginUserVO.getLevel() : 1;
        int points = loginUserVO != null && loginUserVO.getPoints() != null ? loginUserVO.getPoints() : 0;

        return Sender.builder()
                .id(String.valueOf(user.getId()))
                .name(avatar.getAvatarName())
                .avatar(user.getUserAvatar())
                .points(points)
                .level(level)
                .userProfile(user.getUserProfile())
                .avatarFramerUrl(user.getAvatarFramerUrl())
                .titleId(user.getTitleId())
                .titleIdList(user.getTitleIdList())
                .isAdmin(UserConstant.ADMIN_ROLE.equals(user.getUserRole()))
                .isVip(userVipService.isUserVip(user.getId()))
                .build();
    }
}
