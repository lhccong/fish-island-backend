package com.github.yuyuanweb.mianshiyaplugin.model.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目答案
 *
 * @author pine
 */
@Data
public class QuestionAnswer implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 内容
     */
    private String content;

    /**
     * 内容类型：0-富文本, 1-Markdown
     */
    private Integer contentType;

    private User user;

}
