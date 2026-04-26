package com.cong.fishisland.model.vo.pet;

import lombok.Data;

import java.io.Serializable;

/**
 * 单个装备属性VO
 *
 * @author cong
 */
@Data
public class SingleEquipStatsVO implements Serializable {

    /**
     * 基础攻击力
     */
    private Integer baseAttack;

    /**
     * 基础防御力
     */
    private Integer baseDefense;

    /**
     * 基础生命值
     */
    private Integer baseHp;

    /**
     * 速度（决定战斗先手）
     */
    private Integer speed = 0;

    /**
     * 主动属性 - 暴击率
     */
    private Double critRate;

    /**
     * 主动属性 - 连击率
     */
    private Double comboRate;

    /**
     * 主动属性 - 闪避率
     */
    private Double dodgeRate;

    /**
     * 主动属性 - 格挡率
     */
    private Double blockRate;

    /**
     * 主动属性 - 吸血率
     */
    private Double lifesteal;

    /**
     * 抗性属性 - 抗暴击率
     */
    private Double critResistance;

    /**
     * 抗性属性 - 抗连击率
     */
    private Double comboResistance;

    /**
     * 抗性属性 - 抗闪避率
     */
    private Double dodgeResistance;

    /**
     * 抗性属性 - 抗格挡率
     */
    private Double blockResistance;

    /**
     * 抗性属性 - 抗吸血率
     */
    private Double lifestealResistance;

    private static final long serialVersionUID = 1L;
}
