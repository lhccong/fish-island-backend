package com.cong.fishisland.service.impl.chat;

import com.alibaba.fastjson.JSON;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.ws.request.MessageWrapper;
import java.util.Objects;
import java.util.regex.Pattern;

/** Revoke permissions are based on persisted message content, never client-provided type. */
public final class MessageRevokePolicy {
    private static final Pattern REWARD = Pattern.compile(
            "\\[(redpacket|luckybag)\\]\\s*[a-z0-9_-]{1,128}\\s*\\[/\\1\\]", Pattern.CASE_INSENSITIVE);

    private MessageRevokePolicy() {}

    public static boolean canRevoke(RoomMessage message, long userId, String role) {
        if (message == null) return false;
        if (UserConstant.ADMIN_ROLE.equals(role)) return true;
        if (!Objects.equals(message.getUserId(), userId)) return false;
        try {
            MessageWrapper stored = JSON.parseObject(message.getMessageJson(), MessageWrapper.class);
            if (stored == null || stored.getMessage() == null || stored.getMessage().getContent() == null)
                return false;
            return !REWARD.matcher(stored.getMessage().getContent()).find();
        } catch (RuntimeException invalidMessage) {
            return false;
        }
    }
}
