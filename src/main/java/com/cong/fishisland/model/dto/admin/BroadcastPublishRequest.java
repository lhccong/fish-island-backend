package com.cong.fishisland.model.dto.admin;

import lombok.Data;

/** 即时播报发布请求，仅接收正文，不接受客户端指定身份或消息类型。 */
@Data
public class BroadcastPublishRequest {
    private String content;
}
