package com.cong.fishisland.model.vo.vote;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 投票选项VO
 */
@Data
public class VoteOptionVO implements Serializable {

    /**
     * 选项索引
     */
    private Integer index;

    /**
     * 选项文本
     */
    private String text;

    /**
     * 票数
     */
    private Long count;

    /**
     * 百分比
     */
    private Double percentage;

    private static final long serialVersionUID = 1L;
}
