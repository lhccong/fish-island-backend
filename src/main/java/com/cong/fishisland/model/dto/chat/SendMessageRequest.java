package com.cong.fishisland.model.dto.chat;

import lombok.Data;

/** Only message content and reference identity are accepted from the client. */
@Data
public class SendMessageRequest {
    private String content;
    private String quotedMessageId;
    private String clientMessageId;
}
