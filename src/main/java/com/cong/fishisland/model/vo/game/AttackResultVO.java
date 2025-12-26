package com.cong.fishisland.model.vo.game;

import lombok.Data;

import java.io.Serializable;

/**
 * 攻击结果视图对象
 *
 * @author cong
 */
@Data
public class AttackResultVO implements Serializable {

    /**
     * 伤害值
     */
    private int damage;

    /**
     * 是否闪避
     */
    private boolean dodge;

    /**
     * 是否暴击
     */
    private boolean critical;

    /**
     * 是否连击
     */
    private boolean combo;

    private static final long serialVersionUID = 1L;
}

