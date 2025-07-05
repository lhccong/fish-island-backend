package com.cong.fishisland.model.ws.request;

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

}