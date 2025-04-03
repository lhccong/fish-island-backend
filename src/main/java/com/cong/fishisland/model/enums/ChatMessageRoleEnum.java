package com.cong.fishisland.model.enums;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 聊天信息角色枚举
 *
 * @author shing
 */
@Getter
public enum ChatMessageRoleEnum {

    SYSTEM("系统预设","system"),
    USER("用户预设","user"),
    ASSISTANT("助手预设","assistant"),
    FUNCTION("功能预设","function"),
    TOOL("工具预设","tool");


    private final String text;

    @JsonValue
    private final String value;

    ChatMessageRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 从字符串解析枚举
     */
    public static ChatMessageRoleEnum fromValue(String value) {
        for (ChatMessageRoleEnum role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("无效的角色值: " + value);
    }
}
