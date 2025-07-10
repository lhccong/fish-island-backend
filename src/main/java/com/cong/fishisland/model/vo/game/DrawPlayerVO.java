package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.io.Serializable;

/**
 * 你画我猜游戏玩家VO
 *
 * @author cong
 */
@Data
public class DrawPlayerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 是否为房主
     */
    private Boolean isCreator;

    /**
     * 是否为当前绘画者
     */
    private Boolean isCurrentDrawer;

    /**
     * 是否已猜中
     */
    private Boolean hasGuessedCorrectly;

    /**
     * 得分
     */
    private Integer score;
} 