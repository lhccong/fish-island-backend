package com.cong.fishisland.model.dto.user;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 称号查询请求
 * @author 许林涛
 * @date 2025年07月28日 15:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserTitleQueryRequest extends PageRequest implements Serializable {

    /**
     * 称号 ID
     */
    private Long titleId;

    /**
     * 称号名称
     */
    private String name;

    private static final long serialVersionUID = 1L;
}
