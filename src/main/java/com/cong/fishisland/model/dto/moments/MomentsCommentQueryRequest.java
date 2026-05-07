package com.cong.fishisland.model.dto.moments;

import com.cong.fishisland.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 评论分页查询请求
 *
 * @author cong
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "MomentsCommentQueryRequest", description = "朋友圈评论分页查询请求")
public class MomentsCommentQueryRequest extends PageRequest implements Serializable {

    @ApiModelProperty(value = "动态ID", required = true)
    private Long momentId;

    private static final long serialVersionUID = 1L;
}
