package com.cong.fishisland.game.model.dto.response;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 游戏消息结果
 * 统一的消息格式，data 直接存储对象
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessageResult {

    private String type;
    private int code;
    private String message;
    private Object data;

    public static GameMessageResult success(String type, Object data) {
        return GameMessageResult.builder()
                .type(type)
                .code(0)
                .message("success")
                .data(data)
                .build();
    }

    public static GameMessageResult error(String type, String message) {
        return GameMessageResult.builder()
                .type(type)
                .code(-1)
                .message(message)
                .data(null)
                .build();
    }

    /**
     * 转换为 JSON 字符串
     * FastJSON2 会把 Long 转为字符串避免精度丢失
     */
    public String toJson() {
        return JSON.toJSONString(this, JSONWriter.Feature.WriteLongAsString);
    }
}
