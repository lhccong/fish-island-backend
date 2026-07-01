package com.cong.fishisland.websocket.listener;


import com.cong.fishisland.datasource.ai.AIChatDataSource;
import com.cong.fishisland.datasource.ai.SiliconFlowDataSource;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.service.impl.chat.KoishiWebSocketService;
import com.cong.fishisland.service.impl.chat.RobotChatMessageService;
import com.cong.fishisland.websocket.event.AIAnswerEvent;
import com.cong.fishisland.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * 机器人回答消息监听器
 *
 * @author zhongzb create on 2022/08/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIQuestionAnswerListener {
    private final KoishiWebSocketService koishiWebSocketService;
    private final WebSocketService webSocketService;
    @Qualifier("siliconFlowDataSource")
    private final AIChatDataSource siliconFlowDataSource;
    @Qualifier("chutesAI2DataSource")
    private final AIChatDataSource chutesAI2DataSource;
    private final RoomMessageService roomMessageService;
    private final RobotChatMessageService robotChatMessageService;

    // 系统预设
    private final String SYSTEM_PROMPT = "你是摸鱼小助手，你的任务是负责解决摸鱼用户的各种问题，" +
            "你比较擅长配合 emoji 以及清晰易懂的方式回答用户";

    @Async
    @EventListener(classes = AIAnswerEvent.class)
    public void sendAnswer(AIAnswerEvent event) {
        MessageWrapper messageDto = event.getMessageDto();
        Message message = messageDto.getMessage();
        String content = message.getContent().trim().replace("@摸鱼助手", "");

//        koishiWebSocketService.sendMessage(message.getSender().getId(), content, message);
        sendAiFallbackAnswer(content, message);
//        koishiWebSocketService.getKoishiReply(message.getSender().getId(), content, message, koishiReply -> {
//            if (!StringUtils.hasText(koishiReply)) {
//                sendAiFallbackAnswer(content, message);
//            }
//        });
    }

    private void sendAiFallbackAnswer(String content, Message message) {
        List<SiliconFlowRequest.Message> messages = new ArrayList<>();

        messages.add(new SiliconFlowRequest.Message() {{
            setRole("user");
            setContent(content);
        }});

        List<SiliconFlowRequest.Message> requestMessages = new ArrayList<>(messages);
        requestMessages.add(0, new SiliconFlowRequest.Message() {{
            setRole("system");
            setContent(SYSTEM_PROMPT);
        }});

        AiResponse aiResponse = siliconFlowDataSource.getAiResponse(requestMessages, "Qwen/Qwen2.5-14B-Instruct");

        SiliconFlowRequest.Message assistantMessage = new SiliconFlowRequest.Message() {{
            setRole("assistant");
            setContent(aiResponse.getAnswer());
        }};
        messages.add(assistantMessage);

        robotChatMessageService.sendAndSaveAiReply(aiResponse.getAnswer(), message);
    }

}
