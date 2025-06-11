package com.cong.fishisland.model.dto.ws;

import com.cong.fishisland.model.ws.response.UserChatResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WSSchannel 额外 DTO
 * Description: 记录和前端连接的一些映射信息
 * @author cong
 * @date 2023/10/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WSChannelExtraDTO {
    /**
     * 前端如果登录了，记录uid
     */
    private Long uid;

    /**
     * 前端如果登录了，记录用户信息
     */
    private UserChatResponse userChatResponse;
}
