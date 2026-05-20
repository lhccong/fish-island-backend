package com.cong.fishisland.model.dto.datasource;

import com.cong.fishisland.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 数据源 Cookie 分页查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DataSourceCookieQueryRequest extends PageRequest implements Serializable {

    private Long id;

    private String dataSourceKey;

    private Integer status;

    private static final long serialVersionUID = 1L;
}
