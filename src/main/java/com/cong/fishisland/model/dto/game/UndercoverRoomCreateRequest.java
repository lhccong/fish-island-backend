package com.cong.fishisland.model.dto.game;

import lombok.Data;

/**
 * 创建谁是卧底游戏房间请求
 *
 * @author cong
 */
@Data
public class UndercoverRoomCreateRequest {

    /**
     * 平民词语
     */
    private String civilianWord;

    /**
     * 卧底词语
     */
    private String undercoverWord;

    /**
     * 游戏持续时间（秒）
     */
    private Integer duration;
} 