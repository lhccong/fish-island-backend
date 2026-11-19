package com.cong.fishisland.model.ws.request;

import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import com.cong.fishisland.model.vo.user.ChatBubbleArtwork;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * @author cong
 */
@Data
public class Message {
    @JsonProperty("id")
    private String id;

    private String clientMessageId;

    private Poke poke;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String bubbleId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ChatBubbleArtwork bubbleArtwork;

    @JsonProperty("roomId")
    private String roomId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("sender")
    private Sender sender;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("quotedMessage")
    private Message quotedMessage;

    @JsonProperty("mentionedUsers")
    private List<Sender> mentionedUsers;

    public void applyBubble(ChatBubbleProduct product) {
        bubbleId = product == null ? null : product.getId();
        bubbleArtwork = product == null ? null : product.artwork();
    }
}
