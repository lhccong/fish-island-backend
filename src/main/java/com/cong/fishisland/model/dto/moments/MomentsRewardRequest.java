package com.cong.fishisland.model.dto.moments;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 打赏朋友圈请求
 *
 * @author cong
 */
@Data
@ApiModel(value = "MomentsRewardRequest", description = "打赏朋友圈动态请求")
public class MomentsRewardRequest implements Serializable {

    @ApiModelProperty(value = "动态ID", required = true)
    private Long momentId;

    @ApiModelProperty(value = "打赏积分数量（消耗 usedPoints）", required = true, example = "10")
    private Integer points;

    private static final long serialVersionUID = 1L;
}
