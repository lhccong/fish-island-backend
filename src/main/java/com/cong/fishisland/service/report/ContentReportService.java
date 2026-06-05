package com.cong.fishisland.service.report;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cong.fishisland.model.dto.report.ReportAddRequest;
import com.cong.fishisland.model.dto.report.ReportHandleRequest;
import com.cong.fishisland.model.dto.report.ReportQueryRequest;
import com.cong.fishisland.model.entity.report.ContentReport;
import com.cong.fishisland.model.vo.report.ReportReasonOptionVO;
import com.cong.fishisland.model.vo.report.ReportVO;

import java.util.List;

/**
 * 内容举报 Service
 *
 * @author cong
 */
public interface ContentReportService extends IService<ContentReport> {

    /**
     * 提交举报
     */
    Long addReport(ReportAddRequest request);

    /**
     * 获取举报原因选项列表
     */
    List<ReportReasonOptionVO> listReasonOptions();

    /**
     * 分页查询举报记录（管理员）
     */
    Page<ReportVO> listReportPage(ReportQueryRequest request);

    /**
     * 处理举报（管理员）
     */
    boolean handleReport(ReportHandleRequest request);
}
