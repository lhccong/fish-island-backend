package com.cong.fishisland.service;

import com.cong.fishisland.model.dto.vote.VoteAddRequest;
import com.cong.fishisland.model.dto.vote.VoteRecordRequest;
import com.cong.fishisland.model.vo.vote.VoteVO;

import java.util.List;

/**
 * 投票服务接口
 */
public interface VoteService {

    /**
     * 创建投票
     *
     * @param voteAddRequest 投票请求
     * @param userId 用户ID
     * @return 投票ID
     */
    String createVote(VoteAddRequest voteAddRequest, Long userId);

    /**
     * 参与投票
     *
     * @param voteRecordRequest 投票请求
     * @param userId 用户ID
     */
    void vote(VoteRecordRequest voteRecordRequest, Long userId);

    /**
     * 获取投票结果
     *
     * @param voteId 投票ID
     * @param userId 当前用户ID（可为空）
     * @return 投票结果
     */
    VoteVO getVoteResult(String voteId, Long userId);

    /**
     * 获取活跃投票列表
     *
     * @return 投票ID列表
     */
    List<String> getActiveVoteIds();

    /**
     * 删除投票
     *
     * @param voteId 投票ID
     * @param userId 用户ID
     */
    void deleteVote(String voteId, Long userId);
}
