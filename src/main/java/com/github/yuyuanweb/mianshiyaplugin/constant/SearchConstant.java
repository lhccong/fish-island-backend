package com.github.yuyuanweb.mianshiyaplugin.constant;

/**
 * 搜索默认值常量
 *
 * @author pine
 */
public interface SearchConstant {

    /**
     * 题库分类 id
     */
    Long DEFAULT_QUESTION_BANK_CATEGORY_ID = 0L;

    /**
     * 插件的一些方法不允许 null 值，当 questionBankId 为 null 时，使用的占位值
     */
    Long QUESTION_BANK_NULL_ID = -99L;

}
