package com.cong.fishisland.model.dto.mockInterview;


import lombok.Data;
import java.io.Serializable;

/**
 * 模拟面试事件请求
 */
@Data
public class MockInterviewEventRequest  implements Serializable {

    /**
     * 事件类型
     */
    private String event;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 房间 ID
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
