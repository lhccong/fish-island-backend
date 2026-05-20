package com.cong.fishisland.model.dto.datasource;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据源 Cookie 更新请求
 */
@Data
public class DataSourceCookieUpdateRequest implements Serializable {

    private Long id;

    private String dataSourceKey;

    private String cookieValue;

    private String remark;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
