package com.cong.fishisland.websocket.service;


import com.cong.fishisland.model.ws.request.WSBaseReq;
import com.cong.fishisland.model.ws.response.UserChatResponse;
import com.cong.fishisland.model.ws.response.WSBaseResp;
import io.netty.channel.Channel;

import java.util.List;

/**
 * Web 套接字服务
 *
 * @author cong
 * @date 2023/10/27
 */
public interface WebSocketService {
    /**
     * 处理用户登录请求
     *
     * @param channel 渠道
     */
    void handleLoginReq(Channel channel);

    /**
     * 处理所有ws连接的事件
     *
     * @param channel 渠道
     */
    void connect(Channel channel);

    /**
     * 处理ws断开连接的事件
     *
     * @param channel 渠道
     */
    void removed(Channel channel);


    /**
     * 推动消息给所有在线的人
     *
     * @param wsBaseResp 发送的消息体
     * @param skipUid    需要跳过的人
     */
    void sendToAllOnline(WSBaseResp<?> wsBaseResp, Long skipUid);

    /**
     * 推动消息给所有在线的人
     *
     * @param wsBaseResp 发送的消息体
     */
    void sendToAllOnline(WSBaseResp<?> wsBaseResp);

    void sendToUid(WSBaseResp<?> wsBaseResp, Long uid);

    void sendMessage(Channel channel, WSBaseReq req);

    void sendMessage(String token, WSBaseReq req);

    List<UserChatResponse> getOnlineUserList();
}
