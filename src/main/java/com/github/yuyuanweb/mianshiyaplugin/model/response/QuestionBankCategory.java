package com.github.yuyuanweb.mianshiyaplugin.model.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 题库分类对象 question_bank_category
 *
 * @author pine
 */
@Data
public class QuestionBankCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

}
