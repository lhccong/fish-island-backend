package com.cong.fishisland.model.entity.chat;

import com.cong.fishisland.model.enums.ChatMessageRoleEnum;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder(builderClassName = "ChatMessageBuilder")
public class ChatMessage {

    private final String role;
    private final String content;

    // 自定义构建器（与 Lombok 集成）
    public static class ChatMessageBuilder {
        /**
         * 设置角色（使用 ChatMessageRoleEnum 枚举）
         */
        public ChatMessageBuilder role(ChatMessageRoleEnum roleEnum) {
            this.role = roleEnum.getValue();
            return this;
        }

        /**
         * 设置消息内容（自动去除首尾空格）
         */
        public ChatMessageBuilder content(String content) {
            this.content = (content != null) ? content.trim() : "";
            return this;
        }
    }

}