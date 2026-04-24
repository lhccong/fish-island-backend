package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 爬塔挑战结果 VO
 *
 * @author cong
 */
@Data
public class TowerClimbResultVO implements Serializable {

    @ApiModelProperty("挑战层数")
    private Integer floor;

    @ApiModelProperty("是否胜利")
    private Boolean win;

    @ApiModelProperty("宠物剩余血量")
    private Integer petHpLeft;

    @ApiModelProperty("获得积分奖励")
    private Integer rewardPoints;

    @ApiModelProperty("历史最高通关层数")
    private Integer maxFloor;

    @ApiModelProperty("本次战斗回合详情")
    private List<BattleResultVO> battleRounds;

    private static final long serialVersionUID = 1L;
}
