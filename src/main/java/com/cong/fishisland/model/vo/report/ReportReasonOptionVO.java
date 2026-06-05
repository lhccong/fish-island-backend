package com.cong.fishisland.model.vo.report;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 举报原因选项 VO
 *
 * @author cong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReasonOptionVO implements Serializable {

    @ApiModelProperty(value = "原因类型值")
    private Integer value;

    @ApiModelProperty(value = "原因描述")
    private String text;
}
