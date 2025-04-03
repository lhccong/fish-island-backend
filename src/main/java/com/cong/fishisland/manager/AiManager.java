package com.cong.fishisland.manager;


import cn.hutool.core.util.StrUtil;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.model.entity.chat.ChatMessage;
import com.cong.fishisland.model.enums.ChatMessageRoleEnum;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用的 AI 调用类
 */
@Service
@Data
public class AiManager {

    private static final String DEFAULT_MODEL = "deepseek-v3-0324";

    private final RestTemplate restTemplate;

    private final AIModelConfig aiConfig;

    @Autowired
    public AiManager(RestTemplateBuilder restTemplateBuilder, AIModelConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.restTemplate = restTemplateBuilder
                .defaultHeader("Authorization", aiConfig.getAuthorization())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // 仅用户输入
    public String doChat(String userPrompt) {
        return doChat("", userPrompt, DEFAULT_MODEL);
    }

    // 系统提示 + 用户输入
    public String doChat(String systemPrompt, String userPrompt) {
        return doChat(systemPrompt, userPrompt, DEFAULT_MODEL);
    }

    // 完整参数版
    public String doChat(String systemPrompt, String userPrompt, String model) {
        List<ChatMessage> messages = new ArrayList<>(2);
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(ChatMessage.builder()
                    .role(ChatMessageRoleEnum.SYSTEM)
                    .content(systemPrompt)
                    .build());
        }
        messages.add(ChatMessage.builder()
                .role(ChatMessageRoleEnum.USER)
                .content(userPrompt)
                .build());
        return doChat(messages, model);
    }

    // 消息列表 + 默认模型
    public String doChat(List<ChatMessage> messages) {
        return doChat(messages, DEFAULT_MODEL);
    }


    /**
     * 核心请求方法（支持自定义消息列表）
     */
    public String doChat(List<ChatMessage> messages, String model) {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList()));
        requestBody.put("stream", false);

        try {
            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    aiConfig.getUrl(),
                    requestBody,
                    Map.class
            );

            // 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回无效响应");
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 转换消息对象
     */
    private Map<String, String> convertMessage(ChatMessage message) {
        Map<String, String> map = new HashMap<>();
        map.put("role", message.getRole().toLowerCase());
        map.put("content", message.getContent());
        return map;
    }
}
