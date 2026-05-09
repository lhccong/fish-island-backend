package com.cong.fishisland.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.entity.user.UserSignIn;
import com.cong.fishisland.model.vo.user.MonthSignInVO;
import com.cong.fishisland.model.vo.user.SignInStatusVO;
import com.cong.fishisland.model.vo.user.SignInVO;

import java.time.LocalDate;

/**
 * 用户签到 Service
 *
 * @author cong
 */
public interface UserSignInService extends IService<UserSignIn> {

    /**
     * 每日签到（保持原有接口不变，内部同步写入签到记录）
     *
     * @return 签到结果 VO，已签到返回 null
     */
    SignInVO signIn();

    /**
     * 写入签到记录并计算连续天数和奖励（由 UserPointsService 调用，不重复处理积分）
     *
     * @param userId   用户ID
     * @param signDate 签到日期
     * @return 签到结果 VO（bonusPoints 为连续奖励积分，积分发放由调用方负责）
     */
    SignInVO recordSignIn(Long userId, LocalDate signDate);

    /**
     * 补签
     *
     * @param signDate 补签日期，格式 yyyy-MM-dd
     * @return 补签结果 VO
     */
    SignInVO makeUpSignIn(String signDate);

    /**
     * 查询当前用户签到状态
     *
     * @return 签到状态 VO
     */
    SignInStatusVO getSignInStatus();

    /**
     * 获取指定月份的签到日历数据
     *
     * @param year  年份，为 null 时取当前年
     * @param month 月份（1-12），为 null 时取当前月
     * @return 月度签到日历 VO
     */
    MonthSignInVO getMonthSignIn(Integer year, Integer month);
}
