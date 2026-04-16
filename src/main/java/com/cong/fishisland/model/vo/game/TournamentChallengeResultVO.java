package com.cong.fishisland.model.vo.game;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 武道大会挑战结果
 *
 * @author cong
 */
@Data
public class TournamentChallengeResultVO implements Serializable {

    @ApiModelProperty("是否胜利")
    private Boolean isWin;

    @ApiModelProperty("挑战的目标位数")
    private Integer targetRank;

    @ApiModelProperty("挑战成功后我的排名（失败则为原排名，无排名为null）")
    private Integer myRank;

    @ApiModelProperty("被挑战者的userId（目标坑位有人时）")
    private Long opponentUserId;

    @ApiModelProperty("对战回合详情")
    private List<PetBattleResultVO> rounds;

    private static final long serialVersionUID = 1L;
}
