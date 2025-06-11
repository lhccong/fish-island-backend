package com.cong.fishisland.model.vo.ai;

import lombok.Data;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 硅基流动请求
 * @author cong
 */
@Data
public class SiliconFlowRequest {
    // 默认模型
    private String model = "Qwen/Qwen2.5-14B-Instruct";
    private List<Message> messages;
    private boolean stream = false;
    private int max_tokens = 512;
    private Object stop = null;
    private double temperature = 0.7;
    private double top_p = 0.7;
    private int top_k = 50;
    private double frequency_penalty = 0.5;
    private int n = 1;
    private ResponseFormat response_format = new ResponseFormat();
    private List<Tool> tools = Collections.singletonList(new Tool());

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class ResponseFormat {
        private String type = "text";
    }

    @Data
    public static class Tool {
        private String type = "function";
        private Function function = new Function();
    }

    @Data
    public static class Function {
        private String description = "<string>";
        private String name = "<string>";
        private Map<String, Object> parameters = Collections.emptyMap();
        private boolean strict = false;
    }
}
