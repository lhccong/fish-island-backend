package com.cong.fishisland.model.vo.user;

import lombok.Value;

@Value
public class ChatBubbleVO {
    String id;
    String name;
    int points;
    int durationDays;
    String expiresAt;
    boolean equipped;
    ChatBubbleArtwork artwork;
}
