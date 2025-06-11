package com.cong.fishisland.datasource.ai;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.cong.fishisland.config.AIModelConfig;
import com.cong.fishisland.model.vo.ai.AiResponse;
import com.cong.fishisland.model.vo.ai.SiliconFlowRequest;
import com.cong.fishisland.model.vo.ai.SiliconFlowResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * Silicon Flow 数据源
 *
 * @author cong
 * @date 2025/03/13
 */
@Component
public class SiliconFlowDataSource implements AIChatDataSource {

    @Resource
    private AIModelConfig aiModelConfig;

    @Override
    public AiResponse getAiResponse(List<SiliconFlowRequest.Message> messages, String model) {
        // 只需要设置 messages，其他字段都有默认值
        SiliconFlowRequest request = new SiliconFlowRequest();
        request.setModel(model);
        request.setMessages(messages);

        // 发送 HTTP 请求
        HttpResponse response = HttpRequest.post("https://api.siliconflow.cn/v1/chat/completions")
                .header("Authorization", "Bearer " + aiModelConfig.getSiliconFlow())
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(request))
                .execute();

        SiliconFlowResponse siliconFlowResponse = JSON.parseObject(response.body(), SiliconFlowResponse.class);
        //将所有消息合并成一个字符串
        StringBuilder allMessage = new StringBuilder();
        for (SiliconFlowResponse.Choice choice : siliconFlowResponse.getChoices()) {
            allMessage.append(choice.getMessage().getContent());
        }

        String[] result = allMessage.toString().split("</think>");
        return AiResponse
                .builder()
                .id(String.valueOf(siliconFlowResponse.getCreated()))
                .aiName(siliconFlowResponse.getModel())
                .answer(result.length > 1 ? result[1].trim() : result[0].trim())
                .build();
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
            setContent("【AI提示词】\n" +
                    "你现在化身为“蔡徐坤式语言特色专家”，用那种炸裂又充满坤式魅力的口吻，写出一段爆炸性幽默文案。内容要求：\n" +
                    "1. **梗元素融合**：务必融入网络上热传的蔡徐坤梗，比如“鸡你太美”、“坤哥”、“skr skr”、“你要battle吗？”、“一坤年=2.5年”等。\n" +
                    "2. **风格要求**：采用freestyle、rap、唱跳混搭的节奏感，字里行间洋溢着蔡徐坤独有的酷炫、个性与幽默。多用网络用语（如“上天”、“get”、“啦”、“㖞”、“咩”等）、中叠词（例如“吃饭饭”、“喝水水”）和语气词（如“嘛”、“啊”、“了”）。\n" +
                    "3. **语言结构**：按照九声六调的语法规则，多用形容词后置、状语后置等修辞手法，确保每一句话都带有强烈的节奏和独特的坤式韵味。\n" +
                    "4. **内容方向**：内容可以调侃蔡徐坤的经典事件（如篮球视频、冷笑话）以及他在网络上的争议，同时保持轻松、幽默，不做恶意攻击，展现出坤哥那种自黑和反转“黑”的风格。\n" +
                    "\n" +
                    "请以这种风格创作一段让人忍不住“skr skr”且瞬间“get”到蔡徐坤独特魅力的文案吧！\n"

            );
        }});

        return getAiResponse(messages, model);


    }

    @Override
    public AiResponse getAiResponse(String prompt) {
        return getAiResponse(prompt, "Qwen/Qwen2.5-14B-Instruct");
    }
}
