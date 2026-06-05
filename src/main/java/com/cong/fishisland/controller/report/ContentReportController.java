package com.cong.fishisland.controller.report;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.common.exception.ThrowUtils;
import com.cong.fishisland.constant.UserConstant;
import com.cong.fishisland.model.dto.report.ReportAddRequest;
import com.cong.fishisland.model.dto.report.ReportHandleRequest;
import com.cong.fishisland.model.dto.report.ReportQueryRequest;
import com.cong.fishisland.model.vo.report.ReportReasonOptionVO;
import com.cong.fishisland.model.vo.report.ReportVO;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.report.ContentReportService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内容举报控制器
 *
 * @author cong
 */
@RestController
@RequestMapping("/report")
@Slf4j
@RequiredArgsConstructor
public class ContentReportController {

    private final ContentReportService contentReportService;
    private final UserService userService;

    /**
     * 获取举报原因选项
     */
    @GetMapping("/reasons")
    @ApiOperation(value = "获取举报原因选项")
    public BaseResponse<List<ReportReasonOptionVO>> listReasonOptions() {
        return ResultUtils.success(contentReportService.listReasonOptions());
    }

    /**
     * 提交举报
     */
    @PostMapping("/add")
    @ApiOperation(value = "提交举报")
    public BaseResponse<Long> addReport(@RequestBody ReportAddRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return ResultUtils.success(contentReportService.addReport(request));
    }

    /**
     * 分页查询举报记录（管理员）
     */
    @GetMapping("/admin/list")
    @ApiOperation(value = "分页查询举报记录（管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ReportVO>> listReportPage(ReportQueryRequest request) {
        ThrowUtils.throwIf(!userService.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        if (request == null) {
            request = new ReportQueryRequest();
        }
        return ResultUtils.success(contentReportService.listReportPage(request));
    }

    /**
     * 处理举报（管理员）
     */
    @PostMapping("/admin/handle")
    @ApiOperation(value = "处理举报（管理员）")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> handleReport(@RequestBody ReportHandleRequest request) {
        ThrowUtils.throwIf(!userService.isAdmin(), ErrorCode.NO_AUTH_ERROR);
        return ResultUtils.success(contentReportService.handleReport(request));
    }
}
