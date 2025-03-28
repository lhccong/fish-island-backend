package com.github.yuyuanweb.mianshiyaplugin.model.common;

import com.github.yuyuanweb.mianshiyaplugin.config.GlobalState;
import com.github.yuyuanweb.mianshiyaplugin.constant.PageConstant;
import lombok.Data;

import java.util.Objects;

/**
 * 分页请求
 *
 * @author pine
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private long current = 1;

    /**
     * 页面大小
     */
    // private long pageSize = Objects.requireNonNull(GlobalState.getInstance().getState()).pageSize;
    private long pageSize = PageConstant.PAGE_SIZE;

}
