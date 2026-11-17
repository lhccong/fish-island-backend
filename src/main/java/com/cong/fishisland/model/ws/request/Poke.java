package com.cong.fishisland.model.ws.request;

import lombok.Data;

/** Server-owned snapshot. Never populated from client message data. */
@Data
public class Poke {
    private String targetId;
    private String targetName;
    private String suffix;
}
