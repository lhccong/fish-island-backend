package com.cong.fishisland.model.dto.game;

import lombok.Data;

/**
 * 谁是卧底游戏投票请求
 *
 * @author cong
 */
@Data
public class UndercoverVoteRequest {

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 被投票的用户ID
     */
    private Long targetId;
} 