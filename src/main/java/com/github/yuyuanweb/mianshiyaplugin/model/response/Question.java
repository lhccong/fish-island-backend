package com.github.yuyuanweb.mianshiyaplugin.model.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目视图
 *
 * @author pine
 */
@Data
public class Question implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 编号
     */
    private Long questionNum;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 最佳题目答案
     */
    private QuestionAnswer bestQuestionAnswer;

    /**
     * 难度：简单 = 1，中等 = 3，困难 = 5
     */
    private Integer difficulty;

}
