package com.cong.fishisland.service.redeem.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.mapper.redeem.RedeemCodeMapper;
import com.cong.fishisland.model.dto.redeem.RedeemCodeAddRequest;
import com.cong.fishisland.model.dto.redeem.RedeemCodeQueryRequest;
import com.cong.fishisland.model.dto.redeem.RedeemCodeUseRequest;
import com.cong.fishisland.model.dto.user.UserVipAddRequest;
import com.cong.fishisland.model.entity.redeem.RedeemCode;
import com.cong.fishisland.model.entity.redeem.RedeemCodeRecord;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.redeem.RedeemCodeTypeEnum;
import com.cong.fishisland.model.enums.redeem.RedeemRewardTypeEnum;
import com.cong.fishisland.model.vo.redeem.RedeemCodeUseResultVO;
import com.cong.fishisland.model.vo.redeem.RedeemCodeVO;
import com.cong.fishisland.service.UserPointsService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.UserTitleService;
import com.cong.fishisland.service.UserVipService;
import com.cong.fishisland.service.EventRemindService;
import com.cong.fishisland.service.redeem.RedeemCodeRecordService;
import com.cong.fishisland.service.redeem.RedeemCodeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.REDEEM_CODE;

/**
 * 兑换码 Service 实现
 *
 * @author cong
 */
@Slf4j
@Service
public class RedeemCodeServiceImpl extends ServiceImpl<RedeemCodeMapper, RedeemCode>
        implements RedeemCodeService {

    @Resource
    private RedeemCodeRecordService redeemCodeRecordService;

    @Resource
    private UserPointsService userPointsService;

    @Resource
    private UserService userService;

    @Resource
    private UserTitleService userTitleService;

    @Resource
    private UserVipService userVipService;

    @Resource
    private EventRemindService eventRemindService;

    /**
     * 批量生成最大数量
     */
    private static final int MAX_BATCH_COUNT = 100;

    @Override
    public List<String> addRedeemCode(RedeemCodeAddRequest request) {
        // 参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        Integer type = request.getType();
        ThrowUtils.throwIf(type == null || RedeemCodeTypeEnum.getEnumByValue(type) == null,
                ErrorCode.PARAMS_ERROR, "兑换码类型非法");
        Integer rewardType = request.getRewardType();
        ThrowUtils.throwIf(rewardType == null || RedeemRewardTypeEnum.getEnumByValue(rewardType) == null,
                ErrorCode.PARAMS_ERROR, "奖励类型非法");
        ThrowUtils.throwIf(request.getRewardValue() == null || request.getRewardValue() <= 0,
                ErrorCode.PARAMS_ERROR, "奖励值非法");

        // 批量数量
        int batchCount = Optional.ofNullable(request.getBatchCount()).orElse(1);
        ThrowUtils.throwIf(batchCount <= 0 || batchCount > MAX_BATCH_COUNT,
                ErrorCode.PARAMS_ERROR, "批量生成数量需在 1~" + MAX_BATCH_COUNT + " 之间");

        // 专属码批量时不允许指定同一用户（避免歧义）
        if (type.equals(RedeemCodeTypeEnum.EXCLUSIVE.getValue()) && batchCount > 1
                && request.getTargetUserId() != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "批量生成专属码时不能指定同一目标用户");
        }

        List<String> codeList = new ArrayList<>();
        List<RedeemCode> redeemCodes = new ArrayList<>();

        for (int i = 0; i < batchCount; i++) {
            RedeemCode redeemCode = new RedeemCode();
            // 兑换码：自定义或自动生成（批量时强制自动生成）
            String code;
            if (batchCount == 1 && StringUtils.isNotBlank(request.getCode())) {
                code = request.getCode().trim().toUpperCase();
                // 检查是否重复
                ThrowUtils.throwIf(this.count(new LambdaQueryWrapper<RedeemCode>()
                                .eq(RedeemCode::getCode, code)) > 0,
                        ErrorCode.PARAMS_ERROR, "兑换码已存在：" + code);
            } else {
                code = generateUniqueCode();
            }
            redeemCode.setCode(code);
            redeemCode.setType(type);
            redeemCode.setTargetUserId(request.getTargetUserId());
            redeemCode.setRewardType(rewardType);
            redeemCode.setRewardValue(request.getRewardValue());
            redeemCode.setRewardCount(Optional.ofNullable(request.getRewardCount()).orElse(1));
            redeemCode.setDescription(request.getDescription());
            redeemCode.setExpireTime(request.getExpireTime());
            redeemCode.setStatus(1);
            redeemCode.setUsedCount(0);
            // 专属码固定最大使用次数为1
            if (type.equals(RedeemCodeTypeEnum.EXCLUSIVE.getValue())) {
                redeemCode.setMaxUseCount(1);
            } else {
                redeemCode.setMaxUseCount(Optional.ofNullable(request.getMaxUseCount()).orElse(-1));
            }
            redeemCodes.add(redeemCode);
            codeList.add(code);
        }

        this.saveBatch(redeemCodes);
        return codeList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedeemCodeUseResultVO useRedeemCode(RedeemCodeUseRequest request) {
        ThrowUtils.throwIf(request == null || StringUtils.isBlank(request.getCode()),
                ErrorCode.PARAMS_ERROR, "兑换码不能为空");

        String code = request.getCode().trim().toUpperCase();
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());

        // 查询兑换码
        RedeemCode redeemCode = this.getOne(new LambdaQueryWrapper<RedeemCode>()
                .eq(RedeemCode::getCode, code));
        ThrowUtils.throwIf(redeemCode == null, ErrorCode.NOT_FOUND_ERROR, "兑换码不存在");

        // 校验状态
        ThrowUtils.throwIf(redeemCode.getStatus() == 0, ErrorCode.OPERATION_ERROR, "兑换码已禁用");
        ThrowUtils.throwIf(redeemCode.getStatus() == 2, ErrorCode.OPERATION_ERROR, "兑换码已用完");

        // 校验过期时间
        if (redeemCode.getExpireTime() != null && redeemCode.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码已过期");
        }

        // 专属码：校验目标用户
        if (redeemCode.getType().equals(RedeemCodeTypeEnum.EXCLUSIVE.getValue())
                && redeemCode.getTargetUserId() != null
                && !redeemCode.getTargetUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该兑换码不属于您");
        }

        // 校验是否已兑换过
        ThrowUtils.throwIf(redeemCodeRecordService.hasRedeemed(redeemCode.getId(), userId),
                ErrorCode.OPERATION_ERROR, "您已兑换过该兑换码");

        // 发放奖励
        String message = deliverReward(userId, redeemCode);

        // 写入兑换记录
        RedeemCodeRecord record = new RedeemCodeRecord();
        record.setCodeId(redeemCode.getId());
        record.setCode(code);
        record.setUserId(userId);
        record.setRewardType(redeemCode.getRewardType());
        record.setRewardValue(redeemCode.getRewardValue());
        record.setRewardCount(redeemCode.getRewardCount());
        redeemCodeRecordService.save(record);

        // 更新兑换码使用次数，判断是否用完
        int newUsedCount = redeemCode.getUsedCount() + 1;
        int maxUseCount = redeemCode.getMaxUseCount();
        int newStatus = (maxUseCount != -1 && newUsedCount >= maxUseCount) ? 2 : redeemCode.getStatus();
        this.update(new LambdaUpdateWrapper<RedeemCode>()
                .eq(RedeemCode::getId, redeemCode.getId())
                .set(RedeemCode::getUsedCount, newUsedCount)
                .set(RedeemCode::getStatus, newStatus));

        // 构建返回结果
        RedeemRewardTypeEnum rewardTypeEnum = RedeemRewardTypeEnum.getEnumByValue(redeemCode.getRewardType());
        RedeemCodeUseResultVO resultVO = new RedeemCodeUseResultVO();
        resultVO.setCode(code);
        resultVO.setRewardType(redeemCode.getRewardType());
        resultVO.setRewardTypeName(rewardTypeEnum != null ? rewardTypeEnum.getText() : "");
        resultVO.setRewardValue(redeemCode.getRewardValue());
        resultVO.setRewardCount(redeemCode.getRewardCount());
        resultVO.setMessage(message);

        // 发送系统通知
        eventRemindService.sendSystemNotify(userId, message);

        return resultVO;
    }

    @Override
    public Page<RedeemCodeVO> listRedeemCodePage(RedeemCodeQueryRequest request) {
        long current = request.getCurrent();
        long size = request.getPageSize();

        LambdaQueryWrapper<RedeemCode> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(request.getCode())) {
            queryWrapper.like(RedeemCode::getCode, request.getCode().trim().toUpperCase());
        }
        if (request.getType() != null) {
            queryWrapper.eq(RedeemCode::getType, request.getType());
        }
        if (request.getStatus() != null) {
            queryWrapper.eq(RedeemCode::getStatus, request.getStatus());
        }
        if (request.getRewardType() != null) {
            queryWrapper.eq(RedeemCode::getRewardType, request.getRewardType());
        }
        queryWrapper.orderByDesc(RedeemCode::getCreateTime);

        Page<RedeemCode> page = this.page(new Page<>(current, size), queryWrapper);
        List<RedeemCodeVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Page<RedeemCodeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean deleteRedeemCode(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "ID非法");
        RedeemCode redeemCode = this.getById(id);
        ThrowUtils.throwIf(redeemCode == null, ErrorCode.NOT_FOUND_ERROR, "兑换码不存在");
        return this.removeById(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 发放奖励，返回提示信息
     */
    private String deliverReward(Long userId, RedeemCode redeemCode) {
        RedeemRewardTypeEnum rewardTypeEnum = RedeemRewardTypeEnum.getEnumByValue(redeemCode.getRewardType());
        if (rewardTypeEnum == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "奖励类型异常");
        }
        long rewardValue = redeemCode.getRewardValue();
        int rewardCount = Optional.ofNullable(redeemCode.getRewardCount()).orElse(1);

        switch (rewardTypeEnum) {
            case POINTS:
                // 发放可用积分（通过减少 usedPoints 实现，同时写入积分记录）
                userPointsService.updateUsedPoints(userId, -(int) rewardValue,
                        REDEEM_CODE.getValue(),
                        null,
                        "兑换码奖励：" + rewardValue + " 积分");
                return String.format("恭喜获得 %d 积分", rewardValue);

            case VIP_DAYS:
                // 发放会员天数（月卡类型）
                // 永久会员无需再领取
                if (userVipService.isPermanentVip(userId)) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已是永久会员，无需领取");
                }
                Calendar calendar = Calendar.getInstance();
                // 如果用户已是会员且未过期，在到期时间基础上延长；否则从今天开始
                if (userVipService.isUserVip(userId) && !userVipService.isVipExpired(userId)) {
                    // 获取当前到期时间并延长
                    com.cong.fishisland.model.entity.user.UserVip userVip = userVipService.getOne(
                            new LambdaQueryWrapper<com.cong.fishisland.model.entity.user.UserVip>()
                                    .eq(com.cong.fishisland.model.entity.user.UserVip::getUserId, userId)
                                    .orderByDesc(com.cong.fishisland.model.entity.user.UserVip::getValidDays)
                                    .last("LIMIT 1"));
                    if (userVip != null && userVip.getValidDays() != null) {
                        calendar.setTime(userVip.getValidDays());
                    }
                }
                calendar.add(Calendar.DAY_OF_MONTH, (int) rewardValue);
                UserVipAddRequest vipAddRequest = new UserVipAddRequest();
                vipAddRequest.setUserId(userId);
                vipAddRequest.setType(1); 
                vipAddRequest.setValidDays(calendar.getTime());
                userVipService.createVip(vipAddRequest);
                return String.format("恭喜获得 %d 天会员", rewardValue);

            case TITLE:
                // 发放称号
                boolean titleAdded = userTitleService.addTitleToUser(userId, rewardValue);
                ThrowUtils.throwIf(!titleAdded, ErrorCode.OPERATION_ERROR, "称号发放失败，可能已拥有该称号");
                return "恭喜获得新称号";

            case AVATAR_FRAME:
                // 发放头像框（直接写入用户头像框列表）
                User user = userService.getById(userId);
                ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
                List<String> frameIds = Optional.ofNullable(
                        JSON.parseArray(user.getAvatarFramerList(), String.class))
                        .orElse(new ArrayList<>());
                String frameIdStr = String.valueOf(rewardValue);
                ThrowUtils.throwIf(frameIds.contains(frameIdStr),
                        ErrorCode.OPERATION_ERROR, "您已拥有该头像框");
                frameIds.add(frameIdStr);
                user.setAvatarFramerList(JSON.toJSONString(frameIds));
                userService.updateById(user);
                return "恭喜获得新头像框";

            case PROPS:
                // 道具类型暂时转换为积分（可根据业务扩展）
                log.warn("兑换码道具类型暂不支持，userId={}, rewardValue={}", userId, rewardValue);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "道具类型暂不支持，请联系管理员");

            default:
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未知奖励类型");
        }
    }

    /**
     * 生成唯一兑换码（16位大写字母+数字）
     */
    private String generateUniqueCode() {
        String code;
        int maxRetry = 10;
        do {
            // 格式：XXXX-XXXX-XXXX-XXXX
            code = RandomUtil.randomStringUpper(4) + "-"
                    + RandomUtil.randomStringUpper(4) + "-"
                    + RandomUtil.randomStringUpper(4) + "-"
                    + RandomUtil.randomStringUpper(4);
            maxRetry--;
        } while (maxRetry > 0 && this.count(new LambdaQueryWrapper<RedeemCode>()
                .eq(RedeemCode::getCode, code)) > 0);
        return code;
    }

    /**
     * 实体转 VO
     */
    private RedeemCodeVO toVO(RedeemCode redeemCode) {
        RedeemCodeVO vo = new RedeemCodeVO();
        BeanUtils.copyProperties(redeemCode, vo);
        RedeemCodeTypeEnum typeEnum = RedeemCodeTypeEnum.getEnumByValue(redeemCode.getType());
        vo.setTypeName(typeEnum != null ? typeEnum.getText() : "");
        RedeemRewardTypeEnum rewardTypeEnum = RedeemRewardTypeEnum.getEnumByValue(redeemCode.getRewardType());
        vo.setRewardTypeName(rewardTypeEnum != null ? rewardTypeEnum.getText() : "");
        return vo;
    }
}
