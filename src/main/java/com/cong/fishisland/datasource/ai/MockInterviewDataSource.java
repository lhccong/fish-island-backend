package com.cong.fishisland.datasource.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.model.entity.chat.ChatMessage;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.vo.ai.SiliconFlowResponse;
import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * m模拟面试 AI 数据源
 */
@Component
@Data
public class MockInterviewDataSource implements AIChatDataSource {

    private static final String DEFAULT_MODEL = "deepseek-v3-0324";

    private final AIModelConfig aiModelConfig;

    // 构造函数注入配置
    public MockInterviewDataSource(AIModelConfig aiModelConfig) {
        this.aiModelConfig = aiModelConfig;
    }

    @Override
    public AiResponse getAiResponse(List<SiliconFlowRequest.Message> messages, String model) {
        // 构建请求体
        SiliconFlowRequest request = new SiliconFlowRequest();
        request.setModel(model);
        request.setMessages(messages);

        try {
            // 发送请求
            HttpResponse response = HttpRequest.post(aiModelConfig.getUrl())
                    .header("Authorization", aiModelConfig.getAuthorization())
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(request))
                    .execute();

            // 检查响应体是否为空
            String responseBodyStr = response.body();
            if (responseBodyStr == null || responseBodyStr.isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回无效响应");
            }

            // 解析响应体
            SiliconFlowResponse siliconFlowResponse = JSON.parseObject(responseBodyStr, SiliconFlowResponse.class);
            if (siliconFlowResponse == null
                    || siliconFlowResponse.getChoices() == null
                    || siliconFlowResponse.getChoices().isEmpty()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回无效响应");
            }

            // 合并所有 choice 的消息内容
            StringBuilder allMessage = new StringBuilder();
            for (SiliconFlowResponse.Choice choice : siliconFlowResponse.getChoices()) {
                if (choice != null && choice.getMessage() != null) {
                    allMessage.append(choice.getMessage().getContent());
                }
            }

            if (allMessage.length() == 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 返回无效响应");
            }

            // 根据分隔符提取最终回答内容
            String[] result = allMessage.toString().split("</think>");
            String answer = result.length > 1 ? result[1].trim() : result[0].trim();

            return AiResponse.builder()
                    .id(String.valueOf(siliconFlowResponse.getCreated()))
                    .aiName(siliconFlowResponse.getModel())
                    .answer(answer)
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务调用失败: " + e.getMessage());
        }


    }

    @Override
    public AiResponse getAiResponse(String prompt, String model) {
        List<SiliconFlowRequest.Message> messages = new ArrayList<>();
        messages.add(new SiliconFlowRequest.Message() {{
            setRole("user");
            setContent(prompt);
        }});

        messages.add(new SiliconFlowRequest.Message() {{
            setRole("system");
            setContent("你是一个面试者，你的任务是模拟面试。请根据以下要求生成面试问题：\n");
        }});

        return getAiResponse(messages, DEFAULT_MODEL);
    }

    @Override
    public AiResponse getAiResponse(String prompt) {
        return getAiResponse(prompt, DEFAULT_MODEL);
    }
}
