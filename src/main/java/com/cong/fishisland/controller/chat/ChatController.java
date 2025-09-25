package com.cong.fishisland.controller.chat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.service.impl.chat.FlexChatServiceDemo;
import com.cong.fishisland.model.dto.chat.MessageQueryRequest;
import com.cong.fishisland.model.vo.chat.RoomMessageVo;
import com.cong.fishisland.model.ws.response.UserChatResponse;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.websocket.service.WebSocketService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天控制器
 *
 * @author cong
 * @date 2024/02/19
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@RequiredArgsConstructor
@Api(value = "聊天")
public class ChatController {

    private final RoomMessageService roomMessageService;
    private final WebSocketService webSocketService;

    @Autowired
    private FlexChatServiceDemo flexChatServiceDemo;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatDemo(@RequestParam String prompt) {
        return flexChatServiceDemo.streamChat(prompt);
    }

    @PostMapping("/message/page/vo")
    @ApiOperation(value = "分页获取用户房间消息列表")
    public BaseResponse<Page<RoomMessageVo>> listMessageVoByPage(@RequestBody MessageQueryRequest messageQueryRequest) {
        Page<RoomMessageVo> messageVoPage = roomMessageService.listMessageVoByPage(messageQueryRequest);
        return ResultUtils.success(messageVoPage);
    }

    @GetMapping("/online/user")
    @ApiOperation(value = "获取在线用户列表")
    public BaseResponse<List<UserChatResponse>> getOnlineUserList() {
      return   ResultUtils.success(webSocketService.getOnlineUserList());

    }
}