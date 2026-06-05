package com.cong.fishisland.model.dto.report;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 提交举报请求
 *
 * @author cong
 */
@Data
public class ReportAddRequest {

    @ApiModelProperty(value = "举报类型：1-聊天记录，2-帖子，3-鱼小圈", required = true)
    private Integer reportType;

    @ApiModelProperty(value = "被举报对象ID", required = true)
    private Long targetId;

    @ApiModelProperty(value = "被举报用户ID")
    private Long targetUserId;

    @ApiModelProperty(value = "举报原因类型：1-18", required = true)
    private Integer reasonType;

    @ApiModelProperty(value = "补充说明")
    private String description;
}
