package com.cong.fishisland.model.dto.vote;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建投票请求
 */
@Data
public class VoteAddRequest implements Serializable {

    /**
     * 投票标题
     */
    private String title;

    /**
     * 投票选项列表
     */
    private List<String> options;

    /**
     * 是否单选（true-单选，false-多选）
     */
    private Boolean singleChoice;

    private static final long serialVersionUID = 1L;
}
