package com.github.yuyuanweb.mianshiyaplugin.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 刷题信息视图
 *
 * @author pine
 */
@Data
public class DoQuestionInfoVO implements Serializable {

    /**
     * 题目 id 列表信息
     */
    private List<Long> questionIdList;

    /**
     * 当前题目所属题库中的下标
     */
    private Integer currentQuestionIndex;

    /**
     * 题库的总题目数
     */
    private Integer totalQuestionNum;
}
