package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户爬塔进度 VO
 *
 * @author cong
 */
@Data
public class TowerProgressVO implements Serializable {

    @ApiModelProperty("历史最高通关层数")
    private Integer maxFloor;

    @ApiModelProperty("下一层挑战层数（maxFloor + 1）")
    private Integer nextFloor;

    @ApiModelProperty("下一层怪物信息")
    private TowerFloorMonsterVO nextMonster;

    private static final long serialVersionUID = 1L;
}
