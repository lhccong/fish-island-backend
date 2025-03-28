package com.github.yuyuanweb.mianshiyaplugin.model;

import lombok.Data;

/**
 * 热榜新闻
 */
@Data
public class HotNews {
    /**
     * 标题
     */
    private String title;

    /**
     * 关注数
     */
    private long followerCount;

    /**
     * 链接
     */
    private String url;

    /**
     * 来源平台名称
     */
    private String typeName;
} 