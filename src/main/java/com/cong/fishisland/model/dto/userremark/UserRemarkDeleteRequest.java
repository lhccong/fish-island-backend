package com.cong.fishisland.model.dto.userremark;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author cong
 * @description Delete remark request
 */
@Data
public class UserRemarkDeleteRequest {
    @ApiModelProperty(value = "Remark id", required = true)
    private Long id;
}
