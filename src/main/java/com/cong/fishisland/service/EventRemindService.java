package com.cong.fishisland.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.model.dto.event.EventRemindQueryRequest;
import com.cong.fishisland.model.dto.event.EventRemindStateRequest;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.vo.event.EventRemindVO;

/**
* @author 许林涛
* @description 针对表【event_remind(事件提醒表)】的数据库操作Service
* @createDate 2025-07-09 11:23:10
*/
public interface EventRemindService extends IService<EventRemind> {

    /**
     * 批量设置事件提醒为已读
     */
    Boolean batchSetRead(EventRemindStateRequest request);

    /**
     * 批量删除事件提醒（仅接收者可删除）
     */
    Boolean batchDelete(EventRemindStateRequest request);

    /**
     * 检查是否已存在相同事件
     */
    boolean existsEvent(String action, Long sourceId, Integer sourceType,
                        Long senderId, Long recipientId);

    /**
     * 获取查询包装器
     *
     * @param request 查询请求
     * @return 查询包装器
     */
    QueryWrapper<EventRemind> getQueryWrapper(EventRemindQueryRequest request);

    /**
     * 获取事件提醒VO分页
     *
     * @param eventRemindPage 事件提醒分页
     * @return VO分页
     */
    Page<EventRemindVO> getEventRemindVOPage(Page<EventRemind> eventRemindPage);
    /**
     * 发送系统通知给指定用户
     *
     * @param recipientId   接收者用户ID
     * @param sourceContent 通知内容
     */
    void sendSystemNotify(Long recipientId, String sourceContent);

    /**
     * 发送系统通知给指定用户（支持自定义 action，用于幂等查重）
     *
     * @param recipientId   接收者用户ID
     * @param sourceContent 通知内容
     * @param action        动作标识，用于 existsEvent 查重
     */
    void sendSystemNotify(Long recipientId, String sourceContent, String action);
}
