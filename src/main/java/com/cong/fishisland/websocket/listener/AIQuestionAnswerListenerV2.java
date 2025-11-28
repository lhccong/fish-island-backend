package com.cong.fishisland.websocket.listener;


import com.alibaba.fastjson.JSON;
import com.cong.fishisland.datasource.ai.AIChatDataSource;
import com.cong.fishisland.manager.AiManager;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.model.ws.request.Sender;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.event.AIAnswerEvent;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
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
 * 机器人回答消息监听器
 *
 * @author zhongzb create on 2022/08/26
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class AIQuestionAnswerListenerV2 {
    private final WebSocketService webSocketService;
    @Qualifier("siliconFlowDataSource")
    private final AIChatDataSource siliconFlowDataSource;
    @Qualifier("chutesAI2DataSource")
    private final AIChatDataSource chutesAI2DataSource;
    private final AiManager aiManager;
    private final RoomMessageService roomMessageService;
    // 系统预设
    private final String SYSTEM_PROMPT = "你是摸鱼小助手，你的任务是负责解决摸鱼用户的各种问题，" +
            "你比较擅长配合 emoji 以及清晰易懂的方式回答用户";


    @Async
    @EventListener(classes = AIAnswerEvent.class)
    public void sendAnswer(AIAnswerEvent event) {
        MessageWrapper messageDto = event.getMessageDto();
        Message message = messageDto.getMessage();
        String senderId = message.getSender().getId();
        String content = message.getContent().trim().replace("@摸鱼助手", "");
        if (content.contains("我是真爱粉:")) {
            String imgContent = content.replace("我是真爱粉:", "");
            AiResponse aiResponse = chutesAI2DataSource.getAiResponse(imgContent, "flux.1-dev");
            //AI 回答
            sendAndSaveAiMessage("[img]" + aiResponse.getAnswer() + "[/img]", message);
            return;
        }

        // 获取或初始化消息列表
        List<SiliconFlowRequest.Message> messages = new ArrayList<>();

        // 添加当前用户消息
        messages.add(new SiliconFlowRequest.Message() {{
            setRole("user");
            setContent(content);
        }});

        // 调用 AI 添加系统消息
        List<SiliconFlowRequest.Message> requestMessages = new ArrayList<>(messages);
        requestMessages.add(0, new SiliconFlowRequest.Message() {{
            setRole("system");
            setContent(SYSTEM_PROMPT);
        }});

        // 调用 AI
//        AiResponse aiResponse = siliconFlowDataSource.getAiResponse(requestMessages, "Qwen/Qwen2.5-14B-Instruct");
        String aiResult = aiManager.doChat("", content);
        // 添加 AI 响应消息
        SiliconFlowRequest.Message assistantMessage = new SiliconFlowRequest.Message() {{
            setRole("assistant");
            setContent(aiResult);
        }};
        messages.add(assistantMessage);

        //AI 回答
        sendAndSaveAiMessage(aiResult, message);

    }

    private void sendAndSaveAiMessage(String answer, Message message) {
        MessageWrapper messageWrapper = getMessageWrapper(answer, message);

        webSocketService.sendToAllOnline(WSBaseResp.builder()
                .type(MessageTypeEnum.CHAT.getType())
                .data(messageWrapper).build());
        //保存消息到数据库
        RoomMessage roomMessage = new RoomMessage();
        roomMessage.setUserId(-1L);
        roomMessage.setRoomId(-1L);
        roomMessage.setMessageJson(JSON.toJSONString(messageWrapper));
        roomMessage.setMessageId(messageWrapper.getMessage().getId());
        roomMessageService.save(roomMessage);
    }

    private static @NotNull MessageWrapper getMessageWrapper(String answer, Message message) {
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
        aiMessage.setQuotedMessage(message);
        aiMessage.setMentionedUsers(Collections.singletonList(message.getSender()));

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessage(aiMessage);
        return messageWrapper;
    }

}
