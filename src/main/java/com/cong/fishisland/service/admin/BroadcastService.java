package com.cong.fishisland.service.admin;

import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.enums.MessageTypeEnum;
import com.cong.fishisland.model.ws.response.BroadcastResponse;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import com.cong.fishisland.websocket.service.WebSocketService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class BroadcastService {
    private static final Pattern CONTROL = Pattern.compile("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]");
    private final WebSocketService webSocketService;

    public BroadcastService(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    public void publish(String content) {
        // 与前端 String.length 一致，限制 UTF-16 单元；校验原文避免 trim 隐藏控制字符。
        if (content == null || CONTROL.matcher(content).find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入有效的播报文字");
        }
        String normalized = content.replaceAll("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$", "");
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "播报内容需为 1–200 个字符");
        }
        BroadcastResponse event = new BroadcastResponse(UUID.randomUUID().toString(), normalized,
                System.currentTimeMillis());
        webSocketService.sendToAllOnline(WSBaseResp.<BroadcastResponse>builder()
                .type(MessageTypeEnum.BROADCAST.getType()).data(event).build());
    }
}
