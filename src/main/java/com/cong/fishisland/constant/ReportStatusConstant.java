package com.cong.fishisland.constant;

/**
 * 举报处理状态常量
 *
 * @author cong
 */
public interface ReportStatusConstant {

    /**
     * 待处理
     */
    Integer PENDING = 0;

    /**
     * 已处理
     */
    Integer PROCESSED = 1;

    /**
     * 已驳回
     */
    Integer DISMISSED = 2;
}
