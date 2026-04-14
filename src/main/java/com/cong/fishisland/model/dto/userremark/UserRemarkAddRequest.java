package com.cong.fishisland.model.dto.userremark;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author cong
 * @description Add remark request
 */
@Data
public class UserRemarkAddRequest {
    @ApiModelProperty(value = "Remark content", required = true)
    private String content;
}
