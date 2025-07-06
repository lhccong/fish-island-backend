package com.cong.fishisland.service;

/**
 * 异步游戏服务接口
 *
 * @author cong
 */
public interface AsyncGameService {

    /**
     * 按房间存活玩家顺序依次发送发言提醒，发送间隔20秒，全部玩家发送完毕后提醒投票，投票时间30秒后自动结算
     *
     * @param roomId 房间ID
     */
    void startSpeakingAndVoting(String roomId);
} 