package com.cong.fishisland.model.vo.ws;

import lombok.Data;

/**
 * 消息VO
 *
 * @author cong
 * @date 2023/10/31
 */
@Data
public class ChatMessageVo {
    /**
     * 消息类型
     */
    private String type;
    private String content;

    /**
     * 邀请链接在房间已开局时用于匹配观战响应。
     */
    private String watchRequestId;
}
