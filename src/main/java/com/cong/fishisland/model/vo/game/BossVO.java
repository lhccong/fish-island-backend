package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.io.Serializable;

/**
 * Boss视图对象
 *
 * @author cong
 */
@Data
public class BossVO implements Serializable {

    /**
     * Boss ID
     */
    private Long id;

    /**
     * Boss名称
     */
    private String name;

    /**
     * Boss头像
     */
    private String avatar;

    /**
     * Boss血量
     */
    private Integer health;

    /**
     * 击杀Boss的奖励积分
     */
    private Integer rewardPoints;

    /**
     * Boss攻击力
     */
    private Integer attack;

    private static final long serialVersionUID = 1L;
}




