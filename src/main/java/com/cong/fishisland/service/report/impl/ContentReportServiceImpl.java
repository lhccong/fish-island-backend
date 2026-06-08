package com.cong.fishisland.service.report.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.ReportStatusConstant;
import com.cong.fishisland.constant.ReportTypeConstant;
import com.cong.fishisland.mapper.report.ContentReportMapper;
import com.cong.fishisland.model.dto.report.ReportAddRequest;
import com.cong.fishisland.model.dto.report.ReportHandleRequest;
import com.cong.fishisland.model.dto.report.ReportQueryRequest;
import com.cong.fishisland.model.entity.chat.RoomMessage;
import com.cong.fishisland.model.entity.report.ContentReport;
import com.cong.fishisland.model.entity.user.User;
import com.cong.fishisland.model.enums.report.ReportReasonEnum;
import com.cong.fishisland.model.enums.report.ReportStatusEnum;
import com.cong.fishisland.model.enums.report.ReportTypeEnum;
import com.cong.fishisland.model.vo.chat.RoomMessageVo;
import com.cong.fishisland.model.vo.report.ReportReasonOptionVO;
import com.cong.fishisland.model.vo.report.ReportVO;
import com.cong.fishisland.model.vo.user.UserVO;
import com.cong.fishisland.service.RoomMessageService;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.report.ContentReportService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 内容举报 Service 实现
 *
 * @author cong
 */
@Service
public class ContentReportServiceImpl extends ServiceImpl<ContentReportMapper, ContentReport>
        implements ContentReportService {

    private static final int DESCRIPTION_MAX_LENGTH = 1000;

    @Resource
    private RoomMessageService roomMessageService;

    @Resource
    private UserService userService;

    @Override
    public Long addReport(ReportAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request.getReportType()), ErrorCode.PARAMS_ERROR, "举报类型不能为空");
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request.getTargetId()), ErrorCode.PARAMS_ERROR, "被举报对象ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request.getReasonType()), ErrorCode.PARAMS_ERROR, "举报原因不能为空");

        ReportTypeEnum reportTypeEnum = ReportTypeEnum.getEnumByValue(request.getReportType());
        ReportReasonEnum.getEnumByValue(request.getReasonType());

        if (StringUtils.isNotBlank(request.getDescription())
                && request.getDescription().length() > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "补充说明不能超过1000字");
        }

        Long reporterId = StpUtil.getLoginIdAsLong();
        if (reporterId.equals(request.getTargetUserId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能举报自己");
        }

        LambdaQueryWrapper<ContentReport> duplicateWrapper = new LambdaQueryWrapper<>();
        duplicateWrapper.eq(ContentReport::getReporterId, reporterId)
                .eq(ContentReport::getReportType, request.getReportType())
                .eq(ContentReport::getTargetId, request.getTargetId())
                .eq(ContentReport::getIsDelete, 0);
        ThrowUtils.throwIf(this.count(duplicateWrapper) > 0, ErrorCode.OPERATION_ERROR, "您已举报过该内容");

        ContentReport report = new ContentReport();
        report.setReporterId(reporterId);
        report.setReportType(reportTypeEnum.getValue());
        report.setTargetId(request.getTargetId());
        report.setTargetUserId(request.getTargetUserId());
        report.setReasonType(request.getReasonType());
        report.setDescription(StringUtils.trimToNull(request.getDescription()));
        report.setStatus(ReportStatusConstant.PENDING);
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());
        report.setIsDelete(0);

        boolean saved = this.save(report);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "举报提交失败");
        return report.getId();
    }

    @Override
    public List<ReportReasonOptionVO> listReasonOptions() {
        return Arrays.stream(ReportReasonEnum.values())
                .map(item -> ReportReasonOptionVO.builder()
                        .value(item.getValue())
                        .text(item.getText())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Page<ReportVO> listReportPage(ReportQueryRequest request) {
        if (request == null) {
            request = new ReportQueryRequest();
        }

        LambdaQueryWrapper<ContentReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentReport::getIsDelete, 0);
        if (ObjectUtil.isNotEmpty(request.getReportType())) {
            queryWrapper.eq(ContentReport::getReportType, request.getReportType());
        }
        if (ObjectUtil.isNotEmpty(request.getStatus())) {
            queryWrapper.eq(ContentReport::getStatus, request.getStatus());
        }
        if (ObjectUtil.isNotEmpty(request.getReporterId())) {
            queryWrapper.eq(ContentReport::getReporterId, request.getReporterId());
        }
        if (ObjectUtil.isNotEmpty(request.getTargetUserId())) {
            queryWrapper.eq(ContentReport::getTargetUserId, request.getTargetUserId());
        }
        queryWrapper.orderByDesc(ContentReport::getCreateTime);

        Page<ContentReport> reportPage = this.page(new Page<>(request.getCurrent(), request.getPageSize()), queryWrapper);
        Map<String, RoomMessage> chatMessageMap = loadChatMessageMap(reportPage.getRecords());
        Map<Long, UserVO> userMap = loadUserMap(reportPage.getRecords(), chatMessageMap);
        Page<ReportVO> voPage = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        voPage.setRecords(reportPage.getRecords().stream()
                .map(report -> toReportVO(report, chatMessageMap, userMap))
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public boolean handleReport(ReportHandleRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request.getId()), ErrorCode.PARAMS_ERROR, "举报ID不能为空");
        ThrowUtils.throwIf(ObjectUtil.isEmpty(request.getStatus()), ErrorCode.PARAMS_ERROR, "处理状态不能为空");

        ReportStatusEnum statusEnum = ReportStatusEnum.getEnumByValue(request.getStatus());
        ThrowUtils.throwIf(ReportStatusConstant.PENDING.equals(request.getStatus()),
                ErrorCode.PARAMS_ERROR, "处理状态不能为待处理");

        ContentReport report = this.getById(request.getId());
        ThrowUtils.throwIf(report == null || report.getIsDelete() == 1, ErrorCode.NOT_FOUND_ERROR, "举报记录不存在");
        ThrowUtils.throwIf(!ReportStatusConstant.PENDING.equals(report.getStatus()),
                ErrorCode.OPERATION_ERROR, "该举报已处理");

        report.setStatus(statusEnum.getValue());
        report.setHandlerId(StpUtil.getLoginIdAsLong());
        report.setHandleRemark(StringUtils.trimToNull(request.getHandleRemark()));
        report.setHandleTime(new Date());
        report.setUpdateTime(new Date());
        return this.updateById(report);
    }

    private Map<String, RoomMessage> loadChatMessageMap(List<ContentReport> reports) {
        List<Long> chatTargetIds = reports.stream()
                .filter(report -> ReportTypeConstant.CHAT.equals(report.getReportType()))
                .map(ContentReport::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (chatTargetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return roomMessageService.list(new LambdaQueryWrapper<RoomMessage>().in(RoomMessage::getMessageId, chatTargetIds)).stream()
                .collect(Collectors.toMap(RoomMessage::getMessageId, message -> message, (a, b) -> a));
    }

    private Map<Long, UserVO> loadUserMap(List<ContentReport> reports, Map<String, RoomMessage> chatMessageMap) {
        List<Long> userIds = new ArrayList<>();
        for (ContentReport report : reports) {
            if (report.getReporterId() != null) {
                userIds.add(report.getReporterId());
            }
            Long targetUserId = resolveTargetUserId(report, chatMessageMap);
            if (targetUserId != null) {
                userIds.add(targetUserId);
            }
        }
        userIds = userIds.stream().distinct().collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userService.listByIds(userIds);
        return userService.getUserVO(users).stream()
                .collect(Collectors.toMap(UserVO::getId, user -> user, (a, b) -> a));
    }

    private Long resolveTargetUserId(ContentReport report, Map<String, RoomMessage> chatMessageMap) {
        if (report.getTargetUserId() != null) {
            return report.getTargetUserId();
        }
        if (ReportTypeConstant.CHAT.equals(report.getReportType())) {
            RoomMessage roomMessage = chatMessageMap.get(report.getTargetId().toString());
            if (roomMessage != null) {
                return roomMessage.getUserId();
            }
        }
        return null;
    }

    private ReportVO toReportVO(ContentReport report, Map<String, RoomMessage> chatMessageMap, Map<Long, UserVO> userMap) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(report, vo);
        vo.setReportTypeText(ReportTypeEnum.getEnumByValue(report.getReportType()).getText());
        vo.setReasonTypeText(ReportReasonEnum.getEnumByValue(report.getReasonType()).getText());
        vo.setStatusText(ReportStatusEnum.getEnumByValue(report.getStatus()).getText());
        if (report.getReporterId() != null) {
            vo.setReporterUser(userMap.get(report.getReporterId()));
        }
        if (ReportTypeConstant.CHAT.equals(report.getReportType())) {
            RoomMessage roomMessage = chatMessageMap.get(report.getTargetId().toString());
            if (roomMessage != null) {
                vo.setChatMessage(new RoomMessageVo().getVoByEntity(roomMessage));
            }
        }
        Long targetUserId = resolveTargetUserId(report, chatMessageMap);
        if (targetUserId != null) {
            vo.setTargetUserId(targetUserId);
            vo.setTargetUser(userMap.get(targetUserId));
        }
        return vo;
    }
}
