package com.cong.fishisland.model.dto.vote;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 投票请求
 */
@Data
public class VoteRecordRequest implements Serializable {

    /**
     * 投票ID
     */
    private String voteId;

    /**
     * 投票选项索引列表（单选传一个，多选可传多个）
     */
    private List<Integer> optionIndexes;

    private static final long serialVersionUID = 1L;
}
