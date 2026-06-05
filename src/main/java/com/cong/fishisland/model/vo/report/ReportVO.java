package com.cong.fishisland.model.vo.report;

import com.cong.fishisland.model.vo.chat.RoomMessageVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 举报记录 VO
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVO implements Serializable {

    @ApiModelProperty(value = "举报ID")
    private Long id;

    @ApiModelProperty(value = "举报人用户ID")
    private Long reporterId;

    @ApiModelProperty(value = "举报类型")
    private Integer reportType;

    @ApiModelProperty(value = "举报类型描述")
    private String reportTypeText;

    @ApiModelProperty(value = "被举报对象ID")
    private Long targetId;

    @ApiModelProperty(value = "被举报用户ID")
    private Long targetUserId;

    @ApiModelProperty(value = "被举报聊天消息（仅聊天记录举报时有值）")
    private RoomMessageVo chatMessage;

    @ApiModelProperty(value = "举报原因类型")
    private Integer reasonType;

    @ApiModelProperty(value = "举报原因描述")
    private String reasonTypeText;

    @ApiModelProperty(value = "补充说明")
    private String description;

    @ApiModelProperty(value = "处理状态")
    private Integer status;

    @ApiModelProperty(value = "处理状态描述")
    private String statusText;

    @ApiModelProperty(value = "处理人ID")
    private Long handlerId;

    @ApiModelProperty(value = "处理备注")
    private String handleRemark;

    @ApiModelProperty(value = "处理时间")
    private Date handleTime;

    @ApiModelProperty(value = "创建时间")
    private Date createTime;
}
