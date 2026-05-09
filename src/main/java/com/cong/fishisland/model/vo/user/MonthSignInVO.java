package com.cong.fishisland.model.vo.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 月度签到日历 VO
 *
 * @author cong
 */
@Data
@ApiModel(value = "MonthSignInVO", description = "月度签到日历数据")
public class MonthSignInVO {

    // -------- 顶部统计 --------

    /**
     * 连续签到天数
     */
    @ApiModelProperty(value = "连续签到天数")
    private Integer continuousDays;

    /**
     * 累计签到天数（历史总签到次数）
     */
    @ApiModelProperty(value = "累计签到天数")
    private Integer totalSignInDays;

    /**
     * 当前积分
     */
    @ApiModelProperty(value = "当前可用积分")
    private Integer currentPoints;

    // -------- 月历头部 --------

    /**
     * 年份
     */
    @ApiModelProperty(value = "年份", example = "2026")
    private Integer year;

    /**
     * 月份（1-12）
     */
    @ApiModelProperty(value = "月份（1-12）", example = "5")
    private Integer month;

    /**
     * 剩余补签卡数量（本月剩余可补签次数）
     */
    @ApiModelProperty(value = "剩余补签数量")
    private Integer remainMakeUpCount;

    // -------- 日历格子 --------

    /**
     * 本月每天的签到数据，按日期升序排列
     */
    @ApiModelProperty(value = "本月每天签到数据列表")
    private List<DaySignInVO> days;

    /**
     * 单日签到数据
     */
    @Data
    @ApiModel(value = "DaySignInVO", description = "单日签到数据")
    public static class DaySignInVO {

        /**
         * 日（1-31）
         */
        @ApiModelProperty(value = "日（1-31）")
        private Integer day;

        /**
         * 完整日期，格式 yyyy-MM-dd
         */
        @ApiModelProperty(value = "完整日期，格式 yyyy-MM-dd")
        private String date;

        /**
         * 该日预计/实际获得的签到积分奖励
         * - 已签到：实际发放的积分
         * - 未签到：按当前连续天数预测的积分
         */
        @ApiModelProperty(value = "签到积分奖励（已签到为实际值，未签到为预测值）")
        private Integer rewardPoints;

        /**
         * 是否已签到
         */
        @ApiModelProperty(value = "是否已签到")
        private Boolean signed;

        /**
         * 是否是今天
         */
        @ApiModelProperty(value = "是否是今天")
        private Boolean isToday;

        /**
         * 是否可补签（未签到 && 在补签范围内 && 本月补签次数未达上限）
         */
        @ApiModelProperty(value = "是否可补签")
        private Boolean canMakeUp;

        /**
         * 签到类型：1-正常签到，2-补签，null-未签到
         */
        @ApiModelProperty(value = "签到类型：1-正常签到，2-补签，null-未签到")
        private Integer signType;
    }
}
