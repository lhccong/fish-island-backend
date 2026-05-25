package com.cong.fishisland.websocket.event;

import com.cong.fishisland.model.ws.request.MessageWrapper;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 用户 AI 分身回答事件
 */
@Getter
public class UserAiAvatarAnswerEvent extends ApplicationEvent {

    private final MessageWrapper messageDto;
    private final Long avatarUserId;

    public UserAiAvatarAnswerEvent(Object source, MessageWrapper messageDto, Long avatarUserId) {
        super(source);
        this.messageDto = messageDto;
        this.avatarUserId = avatarUserId;
    }
}
