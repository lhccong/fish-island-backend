package com.cong.fishisland.model.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 签到状态 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "SignInStatusVO", description = "签到状态信息")
public class SignInStatusVO {

    /**
     * 今日是否已签到
     */
    @ApiModelProperty(value = "今日是否已签到")
    private Boolean todaySigned;

    /**
     * 当前连续签到天数
     */
    @ApiModelProperty(value = "当前连续签到天数")
    private Integer continuousDays;

    /**
     * 本周期签到状态（true=已签，false=未签），索引 0 对应周期第 1 天
     */
    @ApiModelProperty(value = "本周期签到状态列表（索引0对应第1天）")
    private List<Boolean> weekStatus;

    /**
     * 本月已补签次数
     */
    @ApiModelProperty(value = "本月已补签次数")
    private Integer makeUpCount;

    /**
     * 本月最大补签次数
     */
    @ApiModelProperty(value = "本月最大补签次数")
    private Integer maxMakeUpCount;

    /**
     * 可补签的日期列表（yyyy-MM-dd）
     */
    @ApiModelProperty(value = "可补签的日期列表")
    private List<String> makeUpAvailableDates;
}
