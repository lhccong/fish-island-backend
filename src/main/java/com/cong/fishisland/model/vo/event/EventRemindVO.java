package com.cong.fishisland.model.vo.event;

import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.model.vo.user.UserVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;

/**
 * 事件提醒
 * @author 许林涛
 * @date 2025年07月09日 16:27
 */
@Data
public class EventRemindVO {
    private Long id;
    private String action;
    private Long sourceId;
    private Integer sourceType;
    private String sourceContent;
    private String url;
    private Integer state;
    private Long senderId;
    private Long recipientId;
    private Date remindTime;
    private Date createTime;

    // 发送者用户信息
    private UserVO senderUser;

    public static EventRemindVO objToVo(EventRemind eventRemind) {
        if (eventRemind == null){
            return null;
        }
        EventRemindVO eventRemindVO = new EventRemindVO();
        BeanUtils.copyProperties(eventRemind, eventRemindVO);
        return eventRemindVO;
    }
}
