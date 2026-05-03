package com.cong.fishisland.service.impl.user;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.PointConstant;
import com.cong.fishisland.mapper.user.SignInRewardConfigMapper;
import com.cong.fishisland.mapper.user.UserSignInMapper;
import com.cong.fishisland.model.entity.user.SignInRewardConfig;
import com.cong.fishisland.model.entity.user.UserPoints;
import com.cong.fishisland.model.entity.user.UserSignIn;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.user.MonthSignInVO;
import com.cong.fishisland.model.vo.user.SignInStatusVO;
import com.cong.fishisland.model.vo.user.SignInVO;
import com.cong.fishisland.service.UserPointsRecordService;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserSignInService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author cong
 * @description 针对表【user_sign_in(用户签到记录表)】的数据库操作 Service 实现
 */
@Service
@Slf4j
public class UserSignInServiceImpl extends ServiceImpl<UserSignInMapper, UserSignIn>
        implements UserSignInService {

    /** 每月最大补签次数 */
    private static final int MAX_MAKE_UP_COUNT = 3;
    /** 最多可补签的历史天数 */
    private static final int MAX_MAKE_UP_DAYS = 7;
    /** 补签消耗积分 */
    private static final int MAKE_UP_COST_POINTS = 20;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Lazy
    @Resource
    private UserPointsService userPointsService;

    @Resource
    private UserPointsRecordService userPointsRecordService;

    @Resource
    private SignInRewardConfigMapper signInRewardConfigMapper;

    // ------------------------------------------------------------------ 公开接口

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignInVO signIn() {
        // 委托给 UserPointsService，由它统一处理 Redis 防重、积分发放、签到记录
        return userPointsService.signIn();
    }

    @Override
    public SignInVO recordSignIn(Long userId, LocalDate signDate) {
        return doRecordSignIn(userId, signDate, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SignInVO makeUpSignIn(String signDateStr) {
        Long userId = getLoginUserId();
        LocalDate signDate = LocalDate.parse(signDateStr, DATE_FORMATTER);
        LocalDate today = LocalDate.now();

        // 校验：不能补签今天或未来
        ThrowUtils.throwIf(!signDate.isBefore(today), ErrorCode.PARAMS_ERROR, "只能补签历史日期");
        // 校验：不能超过最大补签范围
        ThrowUtils.throwIf(signDate.isBefore(today.minusDays(MAX_MAKE_UP_DAYS)),
                ErrorCode.PARAMS_ERROR, "只能补签最近 " + MAX_MAKE_UP_DAYS + " 天内的日期");
        // 校验：该日期是否已签到
        ThrowUtils.throwIf(hasSignedOn(userId, signDate), ErrorCode.OPERATION_ERROR, "该日期已签到，无需补签");
        // 校验：本月补签次数
        int makeUpCount = getMakeUpCountThisMonth(userId);
        ThrowUtils.throwIf(makeUpCount >= MAX_MAKE_UP_COUNT,
                ErrorCode.OPERATION_ERROR, "本月补签次数已达上限（" + MAX_MAKE_UP_COUNT + " 次）");

        // 扣除补签消耗积分
        userPointsService.deductPoints(userId, MAKE_UP_COST_POINTS,
                PointsRecordSourceEnum.SIGN_IN_MAKEUP.getValue(), null,
                "补签 " + signDateStr + " 消耗积分");

        return doRecordSignIn(userId, signDate, 2);
    }

    @Override
    public SignInStatusVO getSignInStatus() {
        Long userId = getLoginUserId();
        LocalDate today = LocalDate.now();

        // 今日是否已签到
        boolean todaySigned = hasSignedOn(userId, today);

        // 最新一条记录的连续天数
        UserSignIn latest = getLatestRecord(userId);
        int continuousDays = latest != null ? latest.getContinuousDays() : 0;

        // 本周期签到状态（7天循环）
        List<Boolean> weekStatus = buildWeekStatus(userId, continuousDays);

        // 本月补签次数
        int makeUpCount = getMakeUpCountThisMonth(userId);

        // 可补签日期列表
        List<String> makeUpAvailableDates = buildMakeUpAvailableDates(userId, today);

        SignInStatusVO vo = new SignInStatusVO();
        vo.setTodaySigned(todaySigned);
        vo.setContinuousDays(continuousDays);
        vo.setWeekStatus(weekStatus);
        vo.setMakeUpCount(makeUpCount);
        vo.setMaxMakeUpCount(MAX_MAKE_UP_COUNT);
        vo.setMakeUpAvailableDates(makeUpAvailableDates);
        return vo;
    }

    // ------------------------------------------------------------------ 核心逻辑

    /**
     * 写入签到记录，计算连续天数和奖励，返回 VO（不发放积分，由调用方负责）
     *
     * @param userId   用户ID
     * @param signDate 签到日期
     * @param signType 1-正常签到 2-补签
     */
    private SignInVO doRecordSignIn(Long userId, LocalDate signDate, int signType) {
        // 计算连续天数
        int continuousDays = calcContinuousDays(userId, signDate, signType);

        // 查询连续奖励
        int bonusPoints = getBonusPoints(continuousDays);
        int totalPoints = PointConstant.SIGN_IN_POINT + bonusPoints;

        // 写入签到记录
        UserSignIn record = new UserSignIn();
        record.setUserId(userId);
        record.setSignDate(toDate(signDate));
        record.setSignType(signType);
        record.setContinuousDays(continuousDays);
        record.setRewardPoints(totalPoints);
        this.save(record);

        // 补签时同步发放积分和流水（补签不走 UserPointsService.signIn，需在此处理）
        if (signType == 2) {
            userPointsService.updatePoints(userId, totalPoints, false);
            UserPoints userPoints = userPointsService.getById(userId);
            int afterPoints = userPoints.getPoints();
            int beforePoints = afterPoints - totalPoints;
            int usedPoints = userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints();
            userPointsRecordService.addPointsIncreaseRecord(userId, totalPoints,
                    PointsRecordSourceEnum.SIGN_IN_MAKEUP.getValue(),
                    "补签奖励（" + signDate.format(DATE_FORMATTER) + "，连续第 " + continuousDays + " 天）",
                    beforePoints, afterPoints, usedPoints, usedPoints);
            // 补签连续奖励也加到可用积分
            if (bonusPoints > 0) {
                userPointsService.updateUsedPoints(userId, -bonusPoints);
                UserPoints afterBonus = userPointsService.getById(userId);
                int bonusBeforeUsed = afterBonus.getUsedPoints() + bonusPoints;
                userPointsRecordService.addPointsIncreaseRecord(userId, bonusPoints,
                        PointsRecordSourceEnum.SIGN_IN_MAKEUP.getValue(),
                        "补签连续奖励（第 " + continuousDays + " 天）",
                        afterBonus.getPoints(), afterBonus.getPoints(),
                        bonusBeforeUsed, afterBonus.getUsedPoints());
            }
        }

        // 组装返回 VO
        SignInVO vo = new SignInVO();
        vo.setBasePoints(PointConstant.SIGN_IN_POINT);
        vo.setBonusPoints(bonusPoints);
        vo.setTotalPoints(totalPoints);
        vo.setContinuousDays(continuousDays);
        vo.setSignType(signType);
        vo.setWeekStatus(buildWeekStatus(userId, continuousDays));
        vo.setNextDayBonus(getBonusPoints(continuousDays + 1));
        return vo;
    }

    /**
     * 计算本次签到后的连续天数
     * <p>
     * 正常签到：昨天有记录则 +1，否则重置为 1
     * 补签：插入历史日期后，检查补签日前一天是否有记录
     */
    private int calcContinuousDays(Long userId, LocalDate signDate, int signType) {
        LocalDate prevDate = signDate.minusDays(1);
        UserSignIn prevRecord = getRecordByDate(userId, prevDate);
        if (prevRecord != null) {
            int newDays = prevRecord.getContinuousDays() + 1;
            // 补签后需要级联更新后续记录的连续天数
            if (signType == 2) {
                cascadeUpdateContinuousDays(userId, signDate, newDays);
            }
            return newDays;
        }
        return 1;
    }

    /**
     * 补签后，将该日期之后的连续签到记录的 continuousDays 依次 +1
     */
    private void cascadeUpdateContinuousDays(Long userId, LocalDate fromDate, int startDays) {
        LocalDate cursor = fromDate.plusDays(1);
        int days = startDays + 1;
        while (true) {
            UserSignIn next = getRecordByDate(userId, cursor);
            if (next == null) {
                break;
            }
            next.setContinuousDays(days);
            this.updateById(next);
            cursor = cursor.plusDays(1);
            days++;
        }
    }

    // ------------------------------------------------------------------ 奖励配置

    /**
     * 根据连续天数查询额外奖励积分（支持循环周期）
     */
    private int getBonusPoints(int continuousDays) {
        // 加载所有未删除的配置（数据量小，直接全量）
        List<SignInRewardConfig> configs = signInRewardConfigMapper.selectList(
                new LambdaQueryWrapper<SignInRewardConfig>()
                        .eq(SignInRewardConfig::getIsDelete, 0)
                        .orderByAsc(SignInRewardConfig::getContinuousDays));

        if (configs.isEmpty()) {
            return 0;
        }

        // 取第一条判断是否循环
        SignInRewardConfig first = configs.get(0);
        int cycleDays = (first.getIsCycle() != null && first.getIsCycle() == 1 && first.getCycleDays() != null)
                ? first.getCycleDays() : 0;

        int lookupDays = continuousDays;
        if (cycleDays > 0) {
            // 循环：将天数映射到 [1, cycleDays]
            lookupDays = ((continuousDays - 1) % cycleDays) + 1;
        }

        // 构建 Map 方便查找
        Map<Integer, SignInRewardConfig> configMap = configs.stream()
                .collect(Collectors.toMap(SignInRewardConfig::getContinuousDays, c -> c, (a, b) -> a));

        SignInRewardConfig matched = configMap.get(lookupDays);
        return matched != null ? matched.getRewardPoints() : 0;
    }

    // ------------------------------------------------------------------ 状态查询

    /**
     * 构建本周期（7天）签到状态列表
     */
    private List<Boolean> buildWeekStatus(Long userId, int continuousDays) {
        int cycleDays = 7;
        // 当前在周期中的位置（1-based）
        int posInCycle = continuousDays == 0 ? 0 : ((continuousDays - 1) % cycleDays) + 1;

        List<Boolean> status = new ArrayList<>(cycleDays);
        for (int i = 1; i <= cycleDays; i++) {
            status.add(i <= posInCycle);
        }
        return status;
    }

    /**
     * 构建可补签的日期列表（最近 MAX_MAKE_UP_DAYS 天内未签到的日期）
     */
    private List<String> buildMakeUpAvailableDates(Long userId, LocalDate today) {
        List<String> available = new ArrayList<>();
        for (int i = 1; i <= MAX_MAKE_UP_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            if (!hasSignedOn(userId, date)) {
                available.add(date.format(DATE_FORMATTER));
            }
        }
        return available;
    }

    /**
     * 查询本月已补签次数
     */
    private int getMakeUpCountThisMonth(Long userId) {
        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<UserSignIn>()
                        .eq(UserSignIn::getUserId, userId)
                        .eq(UserSignIn::getSignType, 2)
                        .eq(UserSignIn::getIsDelete, 0)
                        .between(UserSignIn::getSignDate, toDate(start), toDate(end)));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 判断某天是否已签到
     */
    private boolean hasSignedOn(Long userId, LocalDate date) {
        return getRecordByDate(userId, date) != null;
    }

    /**
     * 查询某天的签到记录
     */
    private UserSignIn getRecordByDate(Long userId, LocalDate date) {
        return this.getOne(new LambdaQueryWrapper<UserSignIn>()
                .eq(UserSignIn::getUserId, userId)
                .eq(UserSignIn::getSignDate, toDate(date))
                .eq(UserSignIn::getIsDelete, 0));
    }

    /**
     * 查询最新一条签到记录
     */
    private UserSignIn getLatestRecord(Long userId) {
        return this.getOne(new LambdaQueryWrapper<UserSignIn>()
                .eq(UserSignIn::getUserId, userId)
                .eq(UserSignIn::getIsDelete, 0)
                .orderByDesc(UserSignIn::getSignDate)
                .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 月度日历

    @Override
    public MonthSignInVO getMonthSignIn(Integer year, Integer month) {
        Long userId = getLoginUserId();
        LocalDate today = LocalDate.now();

        // 默认取当前年月
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        // 查询该月所有签到记录，转为 Map<day, UserSignIn>
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        List<UserSignIn> monthRecords = this.list(new LambdaQueryWrapper<UserSignIn>()
                .eq(UserSignIn::getUserId, userId)
                .eq(UserSignIn::getIsDelete, 0)
                .between(UserSignIn::getSignDate, toDate(monthStart), toDate(monthEnd)));

        Map<String, UserSignIn> signedMap = monthRecords.stream()
                .collect(Collectors.toMap(
                        r -> toLocalDate(r.getSignDate()).format(DATE_FORMATTER),
                        r -> r,
                        (a, b) -> a));

        // 本月已补签次数 & 剩余补签卡
        int makeUpCount = (int) monthRecords.stream()
                .filter(r -> r.getSignType() != null && r.getSignType() == 2)
                .count();
        int remainMakeUp = Math.max(0, MAX_MAKE_UP_COUNT - makeUpCount);

        // 顶部统计
        UserSignIn latest = getLatestRecord(userId);
        int continuousDays = latest != null ? latest.getContinuousDays() : 0;

        // 累计签到天数（全量 count，不限月份）
        int totalSignInDays = (int) this.count(new LambdaQueryWrapper<UserSignIn>()
                .eq(UserSignIn::getUserId, userId)
                .eq(UserSignIn::getIsDelete, 0));

        UserPoints userPoints = userPointsService.getById(userId);
        int currentPoints = userPoints != null
                ? (userPoints.getPoints() == null ? 0 : userPoints.getPoints())
                  - (userPoints.getUsedPoints() == null ? 0 : userPoints.getUsedPoints())
                : 0;

        // 构建每日数据
        // 预测连续天数：从最新记录往后推算，用于未签到日的奖励预测
        // 取今天之前最后一次签到的 continuousDays 作为预测基准
        int predictBase = continuousDays;

        List<MonthSignInVO.DaySignInVO> days = new ArrayList<>(monthEnd.getDayOfMonth());
        for (int d = 1; d <= monthEnd.getDayOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            String dateStr = date.format(DATE_FORMATTER);
            boolean isToday = date.equals(today);
            boolean isFuture = date.isAfter(today);

            MonthSignInVO.DaySignInVO dayVO = new MonthSignInVO.DaySignInVO();
            dayVO.setDay(d);
            dayVO.setDate(dateStr);
            dayVO.setIsToday(isToday);

            UserSignIn record = signedMap.get(dateStr);
            if (record != null) {
                // 已签到：使用实际数据
                dayVO.setSigned(true);
                dayVO.setRewardPoints(record.getRewardPoints());
                dayVO.setSignType(record.getSignType());
                dayVO.setCanMakeUp(false);
            } else {
                dayVO.setSigned(false);
                dayVO.setSignType(null);

                if (isFuture) {
                    // 未来日期：按当前连续天数顺延预测奖励
                    long daysFromToday = date.toEpochDay() - today.toEpochDay();
                    int predictDays = predictBase + (int) daysFromToday;
                    dayVO.setRewardPoints(PointConstant.SIGN_IN_POINT + getBonusPoints(Math.max(1, predictDays)));
                    dayVO.setCanMakeUp(false);
                } else {
                    // 历史未签到：按当天应有的连续天数预测（实际已断签，显示基础积分）
                    dayVO.setRewardPoints(PointConstant.SIGN_IN_POINT);
                    // 可补签条件：在补签范围内 && 本月补签次数未达上限
                    boolean inRange = !date.isBefore(today.minusDays(MAX_MAKE_UP_DAYS));
                    dayVO.setCanMakeUp(inRange && makeUpCount < MAX_MAKE_UP_COUNT);
                }
            }
            days.add(dayVO);
        }

        MonthSignInVO vo = new MonthSignInVO();
        vo.setContinuousDays(continuousDays);
        vo.setTotalSignInDays(totalSignInDays);
        vo.setCurrentPoints(currentPoints);
        vo.setYear(ym.getYear());
        vo.setMonth(ym.getMonthValue());
        vo.setRemainMakeUpCount(remainMakeUp);
        vo.setDays(days);
        return vo;
    }

    // ------------------------------------------------------------------ 工具方法

    private Long getLoginUserId() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
