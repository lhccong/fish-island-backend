package com.cong.fishisland.controller.event;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.dto.event.EventRemindQueryRequest;
import com.cong.fishisland.model.dto.event.EventRemindStateRequest;
import com.cong.fishisland.model.entity.event.EventRemind;
import com.cong.fishisland.model.vo.event.EventRemindVO;
import com.cong.fishisland.service.EventRemindService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 事件提醒控制器
 * @author 许林涛
 * @date 2025年07月09日 14:16
 */
@RestController
@RequestMapping("/event_remind")
public class EventRemindController {

    @Resource
    private EventRemindService eventRemindService;


    /**
     * 批量设置事件提醒为已读
     *
     * @param request 批量操作DTO
     * @return 操作结果
     */
    @PostMapping("/batch/set/read")
    @ApiOperation(value = "批量设置事件提醒为已读")
    public BaseResponse<Boolean> batchSetRead(@RequestBody EventRemindStateRequest request) {
        return ResultUtils.success(eventRemindService.batchSetRead(request));
    }

    /**
     * 批量删除事件提醒（仅接收者可删除）
     *
     * @param request 批量操作DTO
     * @return 操作结果
     */
    @PostMapping("/batch/delete")
    @ApiOperation(value = "批量删除事件提醒（仅接收者可删除）")
    public BaseResponse<Boolean> batchDelete(@RequestBody EventRemindStateRequest request) {
        return ResultUtils.success(eventRemindService.batchDelete(request));
    }

    /**
     * 分页获取当前用户的事件提醒列表
     *
     * @param request 事件提醒查询请求
     * @return 分页结果
     */
    @PostMapping("/my/list/page")
    @ApiOperation(value = "分页获取当前用户的事件提醒列表")
    public BaseResponse<Page<EventRemindVO>> listMyEventRemindByPage(@RequestBody EventRemindQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 执行分页查询
        Page<EventRemind> eventRemindPage = eventRemindService.page(
                new Page<>(request.getCurrent(), request.getPageSize()),
                eventRemindService.getQueryWrapper(request)
        );

        // 转换为VO对象
        Page<EventRemindVO> eventRemindVOPage = eventRemindService.getEventRemindVOPage(eventRemindPage);
        return ResultUtils.success(eventRemindVOPage);
    }
}

