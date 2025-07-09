package com.cong.fishisland.model.dto.event;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 事件提醒查询请求
 * @author 许林涛
 * @date 2025年07月09日 16:25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EventRemindQueryRequest extends PageRequest implements Serializable {

    /**
     * 状态（0-未读，1-已读）
     */
    private Integer state;

    /**
     * 动作类型
     */
    private String action;

    private static final long serialVersionUID = 1L;
}

