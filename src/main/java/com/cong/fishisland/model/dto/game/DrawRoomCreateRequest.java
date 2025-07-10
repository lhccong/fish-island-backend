package com.cong.fishisland.model.dto.game;

import lombok.Data;

import java.io.Serializable;

/**
 * 你画我猜游戏房间创建请求
 *
 * @author cong
 */
@Data
public class DrawRoomCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间最大人数（默认8人）
     */
    private Integer maxPlayers;

    /**
     * 总共轮数（默认10轮）
     */
    private Integer totalRounds;
} 