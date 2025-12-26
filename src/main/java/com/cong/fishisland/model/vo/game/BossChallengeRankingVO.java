package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.io.Serializable;

/**
 * Boss挑战排行榜视图对象
 *
 * @author cong
 */
@Data
public class BossChallengeRankingVO implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 宠物名称
     */
    private String petName;

    /**
     * 宠物头像
     */
    private String petAvatar;

    /**
     * 造成的伤害
     */
    private Integer damage;

    /**
     * 排名
     */
    private Integer rank;

    private static final long serialVersionUID = 1L;
}



