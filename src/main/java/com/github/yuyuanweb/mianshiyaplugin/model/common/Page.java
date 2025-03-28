package com.github.yuyuanweb.mianshiyaplugin.model.common;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页
 *
 * @author pine
 */
@Data
public class Page<T> {
    /**
     * 总数
     */
    protected long total;

    /**
     * 每页显示条数，默认 10
     */
    protected long size;

    /**
     * 当前页
     */
    protected long current;

    /**
     * 查询数据列表
     */
    protected List<T> records = Collections.emptyList();

}
