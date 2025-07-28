package com.cong.fishisland.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 称号更新请求
 * @author 许林涛
 * @date 2025年07月28日 15:05
 */
@Data
public class UserTitleUpdateRequest implements Serializable {

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