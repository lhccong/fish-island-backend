package com.cong.fishisland.websocket.listener;


import com.alibaba.fastjson.JSON;
import com.cong.fishisland.datasource.ai.AIChatDataSource;
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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 机器人回答消息监听器（海龟汤）
 *
 * @author zhongzb create on 2022/08/26
 */
//@Slf4j
//@Component
@RequiredArgsConstructor
public class AIPuzzleAnswerListener {
    private final WebSocketService webSocketService;
    private final AIChatDataSource aiChatDataSource;
    private final RoomMessageService roomMessageService;

    // 使用 Caffeine 缓存消息，30 分钟过期
    private final Cache<String, List<SiliconFlowRequest.Message>> globalMessagesCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .build();
    // 系统预设
    private final String SYSTEM_PROMPT = "1. 提供一道海龟汤谜题的“汤面”（故事表面描述）。  \n" +
            "2. 根据玩家的提问，仅回答“是”、“否”或“与此无关”。  \n" +
            "3. 在特定情况下结束游戏并揭示“汤底”（故事真相）。\n" +
            "游戏流程  \n" +
            "1. 当玩家输入“开始”时，你需立即提供一道海龟汤谜题的“汤面”。  \n" +
            "2. 玩家会依次提问，你只能回答以下三种之一：  \n" +
            "  ○ 是：玩家的猜测与真相相符。  \n" +
            "  ○ 否：玩家的猜测与真相不符。  \n" +
            "  ○ 与此无关：玩家的猜测与真相无直接关联。\n" +
            "3. 在以下情况下，你需要主动结束游戏并揭示“汤底”：  \n" +
            "  ○ 玩家明确表示“不想玩了”、“想要答案”或类似表达。  \n" +
            "  ○ 玩家几乎已经还原故事真相，或所有关键问题都已询问完毕。  \n" +
            "  ○ 玩家输入“退出”。  \n" +
            "  ○ 玩家连续提问 10 次仍未触及关键信息，或表现出完全无头绪的状态。\n" +
            "注意事项  \n" +
            "1. 汤面设计：谜题应简短、有趣且逻辑严密，答案需出人意料但合理。  \n" +
            "2. 回答限制：严格遵守“是”、“否”或“与此无关”的回答规则，不得提供额外提示。  \n" +
            "3. 结束时机：在符合结束条件时，及时揭示“汤底”，避免玩家陷入无效推理。\n" +
            "4. 当你决定结束时，必须在结束的消息中包含【游戏已结束】\n" +
            "示例 \n" +
            "● 玩家输入：“开始”  \n" +
            "● AI 回复（汤面）：\n" +
            "“一个人走进餐厅，点了一碗海龟汤，喝了一口后突然冲出餐厅自杀了。为什么？”  \n" +
            "● 玩家提问：“他是因为汤太难喝了吗？”  \n" +
            "● AI 回复：“否。”  \n" +
            "● 玩家提问：“他认识餐厅里的人吗？”  \n" +
            "● AI 回复：“与此无关。”  \n" +
            "● 玩家输入：“退出。”  \n" +
            "● AI 回复（汤底）：\n" +
            "“这个人曾和同伴在海上遇难，同伴死后，他靠吃同伴的尸体活了下来。餐厅的海龟汤让他意识到自己吃的其实是人肉，因此崩溃自杀。”";

    @Async
    @EventListener(classes = AIAnswerEvent.class)
    public void sendAnswer(AIAnswerEvent event) {
        MessageWrapper messageDto = event.getMessageDto();
        Message message = messageDto.getMessage();
        String senderId = message.getSender().getId();
        String content = message.getContent().trim().replace("@摸鱼助手", "");

        // 获取或初始化消息列表
        List<SiliconFlowRequest.Message> messages = Optional.ofNullable(
                globalMessagesCache.get(senderId, k -> new ArrayList<>())
        ).orElseGet(ArrayList::new);

        if ("开始".equals(content.trim())) {
            // 重新开始，清空历史记录
            messages.clear();
        } else if (messages.isEmpty()) {
            sendAndSaveAiMessage("@摸鱼助手 请先输入《开始》开始游戏", message);
            return;
        }

        // 添加当前用户消息
        messages.add(new SiliconFlowRequest.Message() {{
            setRole("user");
            setContent(content);
        }});

        // 调用 AI 时临时添加系统消息
        List<SiliconFlowRequest.Message> requestMessages = new ArrayList<>(messages);
        requestMessages.add(0, new SiliconFlowRequest.Message() {{
            setRole("system");
            setContent(SYSTEM_PROMPT);
        }});

        // 调用 AI
        AiResponse aiResponse = aiChatDataSource.getAiResponse(requestMessages, "Qwen/Qwen2.5-14B-Instruct");

        // 添加 AI 响应消息
        SiliconFlowRequest.Message assistantMessage = new SiliconFlowRequest.Message() {{
            setRole("assistant");
            setContent(aiResponse.getAnswer());
        }};
        messages.add(assistantMessage);

        // 如果游戏结束，移除缓存
        if (aiResponse.getAnswer().contains("游戏已结束")) {
            globalMessagesCache.invalidate(senderId);
        }

        //AI 回答
        sendAndSaveAiMessage(aiResponse.getAnswer(), message);

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
