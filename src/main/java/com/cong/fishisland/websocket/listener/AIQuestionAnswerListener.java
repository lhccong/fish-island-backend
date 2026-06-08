package com.cong.fishisland.websocket.listener;


import com.cong.fishisland.model.ws.request.Message;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import com.cong.fishisland.service.impl.chat.KoishiWebSocketService;
import com.cong.fishisland.websocket.event.AIAnswerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

    @Async
    @EventListener(classes = AIAnswerEvent.class)
    public void sendAnswer(AIAnswerEvent event) {
        MessageWrapper messageDto = event.getMessageDto();
        Message message = messageDto.getMessage();
        String content = message.getContent().trim().replace("@摸鱼助手", "");

        koishiWebSocketService.sendMessage(message.getSender().getId(), content, message);

//        koishiWebSocketService.getKoishiReply(message.getSender().getId(), content, message, koishiReply -> {
//            if (!StringUtils.hasText(koishiReply)) {
//                sendAiFallbackAnswer(content, message);
//            }
//        });
    }

//    private void sendAiFallbackAnswer(String content, Message message) {
//        List<SiliconFlowRequest.Message> messages = new ArrayList<>();
//
//        messages.add(new SiliconFlowRequest.Message() {{
//            setRole("user");
//            setContent(content);
//        }});
//
//        List<SiliconFlowRequest.Message> requestMessages = new ArrayList<>(messages);
//        requestMessages.add(0, new SiliconFlowRequest.Message() {{
//            setRole("system");
//            setContent(SYSTEM_PROMPT);
//        }});
//
//        AiResponse aiResponse = siliconFlowDataSource.getAiResponse(requestMessages, "Qwen/Qwen2.5-14B-Instruct");
//
//        SiliconFlowRequest.Message assistantMessage = new SiliconFlowRequest.Message() {{
//            setRole("assistant");
//            setContent(aiResponse.getAnswer());
//        }};
//        messages.add(assistantMessage);
//
//        robotChatMessageService.sendAndSaveAiReply(aiResponse.getAnswer(), message);
//    }

}
