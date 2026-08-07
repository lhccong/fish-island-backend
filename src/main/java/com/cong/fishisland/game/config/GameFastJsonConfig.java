package com.cong.fishisland.game.config;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 游戏模块 FastJSON2 配置
 * 统一管理游戏相关对象的 JSON 序列化配置
 *
 * @author cong
 */
public class GameFastJsonConfig {

    /**
     * Long 类型字段序列化为字符串，避免 JavaScript 精度丢失
     */
    public static final JSONWriter.Feature[] LONG_AS_STRING = {
            JSONWriter.Feature.WriteLongAsString
    };

    /**
     * 标记 Long 类型字段使用字符串序列化
     */
    public static String serializeLongAsString(long value) {
        return String.valueOf(value);
    }

    private GameFastJsonConfig() {
    }
}
