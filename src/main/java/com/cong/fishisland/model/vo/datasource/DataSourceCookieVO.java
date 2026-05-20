package com.cong.fishisland.model.vo.datasource;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据源 Cookie VO
 */
@Data
public class DataSourceCookieVO implements Serializable {

    private Long id;

    private String dataSourceKey;

    /**
     * 数据源展示名
     */
    private String dataSourceName;

    private String cookieValue;

    private String remark;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
