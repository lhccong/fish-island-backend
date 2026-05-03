package com.cong.fishisland.model.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 签到结果 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "SignInVO", description = "签到结果")
public class SignInVO {

    /**
     * 本次签到获得的基础积分
     */
    @ApiModelProperty(value = "本次签到获得的基础积分")
    private Integer basePoints;

    /**
     * 本次连续奖励额外积分
     */
    @ApiModelProperty(value = "连续签到额外奖励积分")
    private Integer bonusPoints;

    /**
     * 本次签到总积分
     */
    @ApiModelProperty(value = "本次签到总积分")
    private Integer totalPoints;

    /**
     * 当前连续签到天数
     */
    @ApiModelProperty(value = "当前连续签到天数")
    private Integer continuousDays;

    /**
     * 签到类型：1-正常签到，2-补签
     */
    @ApiModelProperty(value = "签到类型：1-正常签到，2-补签")
    private Integer signType;

    /**
     * 本周期签到状态（true=已签，false=未签），索引 0 对应周期第 1 天
     */
    @ApiModelProperty(value = "本周期签到状态列表（索引0对应第1天）")
    private List<Boolean> weekStatus;

    /**
     * 下一天的奖励积分预告
     */
    @ApiModelProperty(value = "下一天连续签到奖励积分预告")
    private Integer nextDayBonus;
}
