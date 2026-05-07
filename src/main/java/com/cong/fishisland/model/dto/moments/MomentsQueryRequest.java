package com.cong.fishisland.model.dto.moments;

import com.cong.fishisland.common.PageRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 朋友圈列表查询请求
 *
 * @author cong
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "MomentsQueryRequest", description = "朋友圈动态分页查询请求")
public class MomentsQueryRequest extends PageRequest implements Serializable {

    @ApiModelProperty(value = "指定查看某个用户的动态，为空则查看好友动态")
    private Long userId;

    private static final long serialVersionUID = 1L;
}
