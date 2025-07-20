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
     * 房间最大人数
     */
    private Integer maxPlayers;

    /**
     * 总轮数
     */
    private Integer totalRounds;
    
    /**
     * 是否仅创建者绘画模式
     */
    private Boolean creatorOnlyMode;
    
    /**
     * 词库类型
     */
    private String wordType;

    private static final long serialVersionUID = 1L;
} 