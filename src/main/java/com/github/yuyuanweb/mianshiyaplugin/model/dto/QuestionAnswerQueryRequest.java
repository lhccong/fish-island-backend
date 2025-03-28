package com.github.yuyuanweb.mianshiyaplugin.model.dto;

import com.github.yuyuanweb.mianshiyaplugin.model.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 *
 * @author pine
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionAnswerQueryRequest extends PageRequest implements Serializable {

    /**
     * 题目 id
     */
    private Long questionId;

    private static final long serialVersionUID = 1L;

}