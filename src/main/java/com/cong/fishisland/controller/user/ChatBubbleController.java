package com.cong.fishisland.controller.user;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.user.ChatBubbleVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.impl.user.ChatBubbleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat/bubble")
@RequiredArgsConstructor
public class ChatBubbleController {
    private final ChatBubbleService bubbles;
    private final UserService users;

    @GetMapping("/list")
    public BaseResponse<List<ChatBubbleVO>> list() {
        return ResultUtils.success(bubbles.list(users.getLoginUser().getId()));
    }

    @PostMapping("/exchange")
    public BaseResponse<List<ChatBubbleVO>> exchange(@RequestBody BubbleRequest request) {
        long userId = users.getLoginUser().getId();
        bubbles.exchange(userId, request.getBubbleId());
        return ResultUtils.success(bubbles.list(userId));
    }

    @PostMapping("/equip")
    public BaseResponse<List<ChatBubbleVO>> equip(@RequestBody BubbleRequest request) {
        long userId = users.getLoginUser().getId();
        bubbles.equip(userId, request.getBubbleId());
        return ResultUtils.success(bubbles.list(userId));
    }

    @Data
    public static class BubbleRequest {
        private String bubbleId;
    }
}
