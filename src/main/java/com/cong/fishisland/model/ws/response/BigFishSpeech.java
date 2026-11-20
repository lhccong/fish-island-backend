package com.cong.fishisland.model.ws.response;

import com.cong.fishisland.model.ws.request.Message;
import lombok.Value;

@Value
public class BigFishSpeech {
    String id;
    String roomId = "-1";
    String status;
    String text;
    Message message;
}
