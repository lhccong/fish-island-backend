package com.cong.fishisland.model.dto.datasource;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据源 Cookie 新增请求
 */
@Data
public class DataSourceCookieAddRequest implements Serializable {

    /**
     * 数据源标识，对应 HotDataKeyEnum.value
     */
    private String dataSourceKey;

    /**
     * Cookie 字符串
     */
    private String cookieValue;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-禁用 1-启用，默认启用
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
