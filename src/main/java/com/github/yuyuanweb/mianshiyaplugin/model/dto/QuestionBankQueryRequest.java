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
public class QuestionBankQueryRequest extends PageRequest implements Serializable {

    /**
     * 标签列表
     */
    private List<String> tagList;

    private static final long serialVersionUID = 1L;
}