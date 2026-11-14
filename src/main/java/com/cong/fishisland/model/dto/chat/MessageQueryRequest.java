package com.cong.fishisland.model.dto.chat;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 聊天查询请求
 *
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MessageQueryRequest extends PageRequest {
    /**
     * 房间 ID
     */
    private Long roomId;

    /**
     * 游标业务消息 ID（room_message.messageId），兼容数字字符串及系统消息 UUID
     */
    private String messageId;
}
