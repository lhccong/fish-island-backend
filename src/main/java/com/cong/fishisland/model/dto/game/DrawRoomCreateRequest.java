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
     * 自定义词语（可选，为空时随机选择）
     */
    private String customWord;
    
    /**
     * 自定义提示词（可选，如：动物、物品、动词等）
     */
    private String wordHint;

    /**
     * 房间最大人数（默认8人）
     */
    private Integer maxPlayers;

    /**
     * 每轮游戏持续时间（秒，默认60秒）
     */
    private Integer roundDuration;
} 