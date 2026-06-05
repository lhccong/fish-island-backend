package com.cong.fishisland.model.dto.report;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 处理举报请求
 *
 * @author cong
 */
@Data
public class ReportHandleRequest {

    @ApiModelProperty(value = "举报ID", required = true)
    private Long id;

    @ApiModelProperty(value = "处理状态：1-已处理，2-已驳回", required = true)
    private Integer status;

    @ApiModelProperty(value = "处理备注")
    private String handleRemark;
}
