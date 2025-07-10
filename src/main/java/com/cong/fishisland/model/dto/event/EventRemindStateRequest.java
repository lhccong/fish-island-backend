package com.cong.fishisland.model.dto.event;

import lombok.Data;

import java.util.List;

/**
 * @author 许林涛
 * @date 2025年07月09日 14:23
 */
@Data
public class EventRemindStateRequest {
    /**
     * 事件提醒ID列表
     */
    private List<Long> ids;
}
