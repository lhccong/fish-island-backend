package com.github.yuyuanweb.mianshiyaplugin.model.dto;

import com.github.yuyuanweb.mianshiyaplugin.constant.SearchConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题库分类和题库关联查询请求
 *
 * @author pine
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionBankCategoryBankQueryRequest extends QuestionBankQueryRequest {

    /**
     * 题库分类 id
     */
    private Long questionBankCategoryId = SearchConstant.DEFAULT_QUESTION_BANK_CATEGORY_ID;

    private static final long serialVersionUID = 1L;
}