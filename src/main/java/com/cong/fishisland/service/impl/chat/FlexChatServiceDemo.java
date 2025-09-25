package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.cong.fishisland.datasource.ai.MockInterviewDataSource.DEFAULT_MODEL;

@Service
public class FlexChatServiceDemo {

    private static final Logger log = LoggerFactory.getLogger(FlexChatServiceDemo.class);
    @Resource
    private WebClient webClient;
    @Resource
    private AIModelConfig aiModelConfig;

    public Flux<String> streamChat(String prompt) {
        return webClient.post()
                .uri(aiModelConfig.getChutesAi2() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "ccong")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(buildRequestBody(prompt))
                .retrieve()
                .bodyToFlux(String.class)
                .takeUntil(line -> line.contains("[DONE]"))
                .map(chunk -> {
                    try {
                        log.info(chunk);

                        String json = chunk.substring("data:".length()).trim();
                        Map<String, Object> map = JSON.parseObject(chunk, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            Object content = delta.get("content");
                            return content != null ? content.toString() : "";
                        }
                    } catch (Exception e) {
                        log.error("Error parsing chunk: {}", chunk, e);
                    }
                    return "";
                })
                .filter(s -> !s.isEmpty());
    }

    // 构建请求体
    private SiliconFlowRequest buildRequestBody(String prompt) {
        SiliconFlowRequest request = new SiliconFlowRequest();
        request.setModel(DEFAULT_MODEL);
        request.setStream(true);

        // 构建消息列表
        List<SiliconFlowRequest.Message> messages = new ArrayList<>();
        SiliconFlowRequest.Message userMessage = new SiliconFlowRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(prompt);
        messages.add(userMessage);

        request.setMessages(messages);

        // 设置其他流式参数（根据 SiliconFlowRequest 默认值）
        request.setMax_tokens(512);
        request.setTemperature(0.7);
        request.setTop_p(0.7);
        // 其他参数保持默认值...

        return request; // 直接返回对象，Jackson 会自动序列化
    }
}
