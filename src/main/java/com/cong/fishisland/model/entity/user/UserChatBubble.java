package com.cong.fishisland.model.entity.user;

import lombok.Data;

@Data
public class UserChatBubble {
    private String bubbleId;
    private long expiresAt;
    private Integer equipped;
}
