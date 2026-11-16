package com.cong.fishisland.model.ws.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BroadcastResponse {
    private String id;
    private String content;
    private long publishedAt;
}
