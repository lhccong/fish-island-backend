package com.cong.fishisland.service.impl.event;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.event.EventRemindMapper;
import com.cong.fishisland.model.dto.event.EventRemindQueryRequest;
import com.cong.fishisland.model.dto.event.EventRemindStateRequest;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.vo.event.EventRemindVO;
import com.cong.fishisland.model.vo.post.PostVO;
import com.cong.fishisland.service.EventRemindService;
import com.cong.fishisland.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 许林涛
 * @description 针对表【event_remind(事件提醒表)】的数据库操作Service实现
 * @createDate 2025-07-09 11:23:10
 */
@Service
public class EventRemindServiceImpl extends ServiceImpl<EventRemindMapper, EventRemind>
        implements EventRemindService {

    @Resource
    private UserService userService;

    /**
     * 已读
     */
    private static final Integer READ = 1;

    @Override
    public Boolean batchSetRead(EventRemindStateRequest request) {
        // 参数校验
        validEventRemindStateRequest(request);
        List<Long> ids = request.getIds();
        User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();
        QueryWrapper<EventRemind> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", request.getIds());
        queryWrapper.eq("recipientId", userId);
        long count = this.count(queryWrapper);
        // 校验权限
        ThrowUtils.throwIf(count != (long) ids.size(), ErrorCode.NO_AUTH_ERROR);
        // 更新数据
        UpdateWrapper<EventRemind> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", request.getIds());
        updateWrapper.set("state", READ);
        return update(updateWrapper);
    }

    @Override
    public Boolean batchDelete(EventRemindStateRequest request) {
        // 参数校验
        validEventRemindStateRequest(request);
        List<Long> ids = request.getIds();
        User loginUser = userService.getLoginUser();
        Long userId = loginUser.getId();
        QueryWrapper<EventRemind> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", request.getIds());
        queryWrapper.eq("recipientId", userId);
        long count = this.count(queryWrapper);
        // 校验权限：只有接收者才能删除自己的提醒
        ThrowUtils.throwIf(count != (long) ids.size(), ErrorCode.NO_AUTH_ERROR);
        // 删除数据
        return this.removeByIds(ids);
    }

    @Override
    public boolean existsEvent(String action, Long sourceId, Integer sourceType,
                               Long senderId, Long recipientId) {
        QueryWrapper<EventRemind> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("action", action)
                .eq("sourceId", sourceId)
                .eq("sourceType", sourceType)
                .eq("senderId", senderId)
                .eq("recipientId", recipientId);
        return this.count(queryWrapper) > 0;
    }


    @Override
    public QueryWrapper<EventRemind> getQueryWrapper(EventRemindQueryRequest request) {
        QueryWrapper<EventRemind> queryWrapper = new QueryWrapper<>();
        if (request == null) {
            return queryWrapper;
        }
        User loginUser = userService.getLoginUser();
        // 添加接收者条件
        queryWrapper.eq("recipientId", loginUser.getId());

        // 添加状态条件
        if (request.getState() != null) {
            queryWrapper.eq("state", request.getState());
        }

        // 添加动作类型条件
        if (StringUtils.isNotBlank(request.getAction())) {
            queryWrapper.eq("action", request.getAction());
        }

        // 按提醒时间倒序排列
        queryWrapper.orderByDesc("remindTime");
        return queryWrapper;
    }

    @Override
    public Page<EventRemindVO> getEventRemindVOPage(Page<EventRemind> eventRemindPage) {
        List<EventRemind> records = eventRemindPage.getRecords();
        Page<EventRemindVO> voPage = new Page<>(eventRemindPage.getCurrent(), eventRemindPage.getSize(), eventRemindPage.getTotal());
        if (CollUtil.isEmpty(records)) {
            return voPage;
        }
        // 批量获取发送者用户信息
        Set<Long> userIdSet = records.stream().map(EventRemind::getSenderId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充发送者用户信息
        List<EventRemindVO> eventRemindVOList = records.stream().map(EventRemindVO::objToVo).collect(Collectors.toList());
        voPage.setRecords(eventRemindVOList);
        voPage.getRecords().forEach(vo -> {
            User user;
            Long userId = vo.getSenderId();
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }else {
                user = new User();
                user.setId(-1L);
                user.setUserName("系统消息");
                user.setUserAvatar("https://s1.aigei.com/src/img/gif/41/411d8d587bfc41aeaadfb44ae246da0d.gif?imageMogr2/auto-orient/thumbnail/!282x282r/gravity/Center/crop/282x282/quality/85/%7CimageView2/2/w/282&e=2051020800&token=P7S2Xpzfz11vAkASLTkfHN7Fw-oOZBecqeJaxypL:OU5w-4wX8swq04CJ3p4N0tl_J7E=");
            }
            vo.setSenderUser(userService.getUserVO(user));
        });
        return voPage;
    }

    private void validEventRemindStateRequest(EventRemindStateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "参数为空");
        List<Long> ids = request.getIds();
        ThrowUtils.throwIf(CollUtil.isEmpty(ids), ErrorCode.PARAMS_ERROR, "请选择要操作的数据");
    }
}




