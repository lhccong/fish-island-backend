package com.cong.fishisland.model.dto.userremark;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author cong
 * @description Update remark request
 */
@Data
public class UserRemarkUpdateRequest {
    @ApiModelProperty(value = "Remark id", required = true)
    private Long id;

    @ApiModelProperty(value = "Remark content", required = true)
    private String content;
}
