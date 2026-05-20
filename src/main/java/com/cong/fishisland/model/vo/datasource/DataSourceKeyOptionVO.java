package com.cong.fishisland.model.vo.datasource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 可选数据源 Key
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceKeyOptionVO implements Serializable {

    private String value;

    private String text;

    private static final long serialVersionUID = 1L;
}
