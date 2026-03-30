package com.cong.fishisland.controller.user;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.entity.user.UserPointsRecord;
import com.cong.fishisland.model.enums.user.PointsRecordSourceEnum;
import com.cong.fishisland.model.vo.user.UserPointsRecordVO;
import com.cong.fishisland.service.UserPointsRecordService;
import static com.cong.fishisland.model.enums.user.PointsRecordSourceEnum.getEnumByValue;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户积分记录接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/user/points/record")
@Slf4j
public class UserPointsRecordController {

    @Resource
    private UserPointsRecordService userPointsRecordService;


    /**
     * 获取当前登录用户的积分记录列表
     *
     * @param current  当前页
     * @param pageSize 每页大小
     * @return {@link BaseResponse}<{@link Page}<{@link UserPointsRecordVO}>>
     */
    @GetMapping("/list/my")
    @ApiOperation(value = "获取当前登录用户的积分记录列表")
    public BaseResponse<Page<UserPointsRecordVO>> listMyPointsRecords(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页大小不能超过50");

        // 查询用户的积分记录
        LambdaQueryWrapper<UserPointsRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPointsRecord::getUserId, userId)
                .orderByDesc(UserPointsRecord::getCreateTime);

        Page<UserPointsRecord> page = userPointsRecordService.page(new Page<>(current, pageSize), queryWrapper);

        // 转换为VO
        List<UserPointsRecordVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<UserPointsRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 根据用户ID获取积分记录列表（仅管理员）
     *
     * @param userId   用户ID
     * @param current  当前页
     * @param pageSize 每页大小
     * @return {@link BaseResponse}<{@link Page}<{@link UserPointsRecordVO}>>
     */
    @GetMapping("/list/user")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @ApiOperation(value = "根据用户ID获取积分记录列表（仅管理员）")
    public BaseResponse<Page<UserPointsRecordVO>> listUserPointsRecords(
            @RequestParam long userId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {
        ThrowUtils.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID非法");

        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页大小不能超过50");

        // 查询用户的积分记录
        LambdaQueryWrapper<UserPointsRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPointsRecord::getUserId, userId)
                .orderByDesc(UserPointsRecord::getCreateTime);

        Page<UserPointsRecord> page = userPointsRecordService.page(new Page<>(current, pageSize), queryWrapper);

        // 转换为VO
        List<UserPointsRecordVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<UserPointsRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 获取当前登录用户的积分记录总数
     *
     * @return {@link BaseResponse}<{@link Long}>
     */
    @GetMapping("/count/my")
    @ApiOperation(value = "获取当前登录用户的积分记录总数")
    public BaseResponse<Long> countMyPointsRecords() {
        Long userId = StpUtil.getLoginIdAsLong();

        long count = userPointsRecordService.lambdaQuery()
                .eq(UserPointsRecord::getUserId, userId)
                .count();

        return ResultUtils.success(count);
    }

    /**
     * 根据来源类型获取当前登录用户的积分记录列表
     *
     * @param sourceType 来源类型
     * @param current    当前页
     * @param pageSize   每页大小
     * @return {@link BaseResponse}<{@link Page}<{@link UserPointsRecordVO}>>
     */
    @GetMapping("/list/my/by-source")
    @ApiOperation(value = "根据来源类型获取当前登录用户的积分记录列表")
    public BaseResponse<Page<UserPointsRecordVO>> listMyPointsRecordsBySource(
            @RequestParam String sourceType,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {
        // 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "每页大小不能超过50");

        // 查询用户的积分记录
        LambdaQueryWrapper<UserPointsRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPointsRecord::getUserId, userId)
                .eq(UserPointsRecord::getSourceType, sourceType)
                .orderByDesc(UserPointsRecord::getCreateTime);

        Page<UserPointsRecord> page = userPointsRecordService.page(new Page<>(current, pageSize), queryWrapper);

        // 转换为VO
        List<UserPointsRecordVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<UserPointsRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);

        return ResultUtils.success(voPage);
    }

    /**
     * 将实体转换为VO
     *
     * @param record 积分记录实体
     * @return {@link UserPointsRecordVO}
     */
    private UserPointsRecordVO convertToVO(UserPointsRecord record) {
        if (record == null) {
            return null;
        }
        UserPointsRecordVO vo = new UserPointsRecordVO();
        BeanUtils.copyProperties(record, vo);

        // 设置变动类型文本
        if (record.getChangeType() != null) {
            vo.setChangeTypeText(record.getChangeType() == 1 ? "增加" : "扣除");
        }

        // 设置来源类型文本
        if (record.getSourceType() != null) {
            PointsRecordSourceEnum sourceEnum = getEnumByValue(record.getSourceType());
            vo.setSourceTypeText(sourceEnum != null ? sourceEnum.getText() : "未知");
        }

        return vo;
    }
}
