package com.cong.fishisland.websocket.event;

import lombok.Value;

@Value
public class BigFishChatEvent {
    String messageId;
    long userId;
    String userName;
    String content;
    String quotedContent;
}
