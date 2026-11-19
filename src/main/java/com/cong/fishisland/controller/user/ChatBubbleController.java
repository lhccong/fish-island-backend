package com.cong.fishisland.controller.user;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.model.entity.user.ChatBubbleProduct;
import com.cong.fishisland.model.vo.user.ChatBubbleVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.impl.user.ChatBubbleCatalog;
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
    private final ChatBubbleCatalog catalog;

    @GetMapping("/list")
    public BaseResponse<List<ChatBubbleVO>> list() {
        return ResultUtils.success(bubbles.list(users.getLoginUser().getId()));
    }

    @PostMapping("/exchange")
    public BaseResponse<List<ChatBubbleVO>> exchange(@RequestBody BubbleRequest request) {
        long userId = users.getLoginUser().getId();
        bubbles.exchange(userId, request.getBubbleId(), request.getExpectedPoints());
        return ResultUtils.success(bubbles.list(userId));
    }

    @PostMapping("/equip")
    public BaseResponse<List<ChatBubbleVO>> equip(@RequestBody BubbleRequest request) {
        long userId = users.getLoginUser().getId();
        bubbles.equip(userId, request.getBubbleId());
        return ResultUtils.success(bubbles.list(userId));
    }

    @GetMapping("/admin/list")
    public BaseResponse<List<ChatBubbleProduct>> products() {
        requireAdmin();
        return ResultUtils.success(catalog.list());
    }

    @PostMapping("/admin/create")
    public BaseResponse<ChatBubbleProduct> create(@RequestBody ProductRequest request) {
        requireAdmin();
        return ResultUtils.success(catalog.create(request.getId(), request.getName(), request.getPoints(),
                request.getImageUrl(), request.getDarkImageUrl(), request.getTextTone()));
    }

    @PostMapping("/admin/price")
    public BaseResponse<ChatBubbleProduct> setPrice(@RequestBody PriceRequest request) {
        requireAdmin();
        return ResultUtils.success(catalog.setPrice(request.getId(), request.getPoints()));
    }

    private void requireAdmin() {
        ThrowUtils.throwIf(!users.isAdmin(users.getLoginUser()), ErrorCode.NO_AUTH_ERROR);
    }

    @Data
    public static class BubbleRequest {
        private String bubbleId;
        private Integer expectedPoints;
    }

    @Data
    public static class ProductRequest {
        private String id;
        private String name;
        private Integer points;
        private String imageUrl;
        private String darkImageUrl;
        private String textTone;
    }

    @Data
    public static class PriceRequest {
        private String id;
        private Integer points;
    }
}
