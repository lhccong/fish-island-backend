package com.cong.fishisland.model.vo.vote;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 投票结果VO
 */
@Data
public class VoteVO implements Serializable {

    /**
     * 投票ID
     */
    private String voteId;

    /**
     * 投票标题
     */
    private String title;

    /**
     * 是否单选
     */
    private Boolean singleChoice;

    /**
     * 总票数
     */
    private Long totalCount;

    /**
     * 选项列表
     */
    private List<VoteOptionVO> options;

    /**
     * 当前用户是否已投票
     */
    private Boolean hasVoted;

    /**
     * 当前用户投票的选项索引列表
     */
    private List<Integer> userVotedOptions;

    /**
     * 剩余时间（秒）
     */
    private Long remainingSeconds;

    private static final long serialVersionUID = 1L;
}
