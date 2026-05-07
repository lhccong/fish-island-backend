package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 爬塔层级怪物信息 VO
 *
 * @author cong
 */
@Data
public class TowerFloorMonsterVO implements Serializable {

    @ApiModelProperty("层数")
    private Integer floor;

    @ApiModelProperty("怪物名称")
    private String name;

    @ApiModelProperty("怪物头像")
    private String avatarUrl = "https://img0.baidu.com/it/u=4184635331,1123213756&fm=253&fmt=auto&app=120&f=JPEG?w=440&h=441";

    @ApiModelProperty("怪物血量")
    private Integer health;

    @ApiModelProperty("怪物攻击力")
    private Integer attack;

    @ApiModelProperty("暴击率")
    private Double critRate;

    @ApiModelProperty("连击率")
    private Double comboRate;

    @ApiModelProperty("闪避率")
    private Double dodgeRate;

    @ApiModelProperty("格挡率")
    private Double blockRate;

    @ApiModelProperty("吸血率")
    private Double lifesteal;

    @ApiModelProperty("通关奖励积分")
    private Integer rewardPoints;

    private static final long serialVersionUID = 1L;
}
