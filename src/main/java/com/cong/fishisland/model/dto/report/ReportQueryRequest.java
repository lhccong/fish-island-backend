package com.cong.fishisland.model.dto.report;

import com.cong.fishisland.common.PageRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 举报分页查询请求
 *
 * @author cong
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReportQueryRequest extends PageRequest implements Serializable {

    @ApiModelProperty(value = "举报类型：1-聊天记录，2-帖子，3-鱼小圈")
    private Integer reportType;

    @ApiModelProperty(value = "处理状态：0-待处理，1-已处理，2-已驳回")
    private Integer status;

    @ApiModelProperty(value = "举报人用户ID")
    private Long reporterId;

    @ApiModelProperty(value = "被举报用户ID")
    private Long targetUserId;
}
