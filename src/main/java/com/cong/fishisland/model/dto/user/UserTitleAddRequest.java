package com.cong.fishisland.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 称号添加请求
 * @author 许林涛
 * @date 2025年07月28日 15:04
 */
@Data
public class UserTitleAddRequest implements Serializable {

    /**
     * 称号名称
     */
    private String name;

    private static final long serialVersionUID = 1L;
}
