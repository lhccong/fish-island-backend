package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 对战结果视图对象
 *
 * @author cong
 */
@Data
public class BattleResultVO implements Serializable {

    /**
     * 当前攻击对象类型：PET-宠物攻击，BOSS-Boss攻击
     */
    @ApiModelProperty(value = "当前攻击对象类型：PET-宠物攻击，BOSS-Boss攻击", example = "PET")
    private String attackerType;

    /**
     * 是否暴击
     */
    @ApiModelProperty(value = "是否暴击", example = "false")
    private Boolean isCritical;

    /**
     * 是否普通攻击
     */
    @ApiModelProperty(value = "是否普通攻击", example = "true")
    private Boolean isNormalAttack;

    /**
     * 是否闪避
     */
    @ApiModelProperty(value = "是否闪避", example = "false")
    private Boolean isDodge;

    /**
     * 是否连击
     */
    @ApiModelProperty(value = "是否连击", example = "false")
    private Boolean isCombo;

    /**
     * 扣血量
     */
    @ApiModelProperty(value = "扣血量", example = "100")
    private Integer damage;

    /**
     * 宠物剩余血量
     */
    @ApiModelProperty(value = "宠物剩余血量", example = "500")
    private Integer petRemainingHealth;

    /**
     * Boss剩余血量
     */
    @ApiModelProperty(value = "Boss剩余血量", example = "8000")
    private Integer bossRemainingHealth;

    private static final long serialVersionUID = 1L;
}




