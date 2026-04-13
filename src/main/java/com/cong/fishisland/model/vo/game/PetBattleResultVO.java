package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 宠物对战结果视图对象
 *
 * @author cong
 */
@Data
public class PetBattleResultVO implements Serializable {

    /**
     * 当前攻击方类型：MY_PET-我的宠物攻击，OPPONENT_PET-对手宠物攻击
     */
    @ApiModelProperty(value = "当前攻击方类型：MY_PET-我的宠物攻击，OPPONENT_PET-对手宠物攻击", example = "MY_PET")
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
     * 我的宠物剩余血量
     */
    @ApiModelProperty(value = "我的宠物剩余血量", example = "500")
    private Integer myPetRemainingHealth;

    /**
     * 对手宠物剩余血量
     */
    @ApiModelProperty(value = "对手宠物剩余血量", example = "500")
    private Integer opponentPetRemainingHealth;

    private static final long serialVersionUID = 1L;
}
