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
     * 游标消息 ID（room_message.id），传入后走游标分页，忽略页码
     */
    private Long messageId;
}