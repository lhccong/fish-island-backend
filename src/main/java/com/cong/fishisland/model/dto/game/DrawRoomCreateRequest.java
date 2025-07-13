package com.cong.fishisland.model.dto.game;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建你画我猜房间请求
 *
 * @author cong
 */
@Data
public class DrawRoomCreateRequest implements Serializable {

    /**
     * 最大玩家数
     */
    private Integer maxPlayers;

    /**
     * 总轮数
     */
    private Integer totalRounds;
    
    /**
     * 房间模式
     * true: 房主绘画模式
     * false: 轮换模式
     */
    private Boolean creatorOnlyMode;

    private static final long serialVersionUID = 1L;
} 