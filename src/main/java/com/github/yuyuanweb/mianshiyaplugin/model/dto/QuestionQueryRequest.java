package com.github.yuyuanweb.mianshiyaplugin.model.dto;

import com.github.yuyuanweb.mianshiyaplugin.model.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查询请求
 *
 * @author pine
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuestionQueryRequest extends PageRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 仅vip可见（1 表示仅会员可见）
     */
    private Integer needVip;

    /**
     * 题库id
     */
    private Long questionBankId;

    /**
     * 难度
     */
    private Integer difficulty;

    private static final long serialVersionUID = 1L;
}