package com.cong.fishisland.controller;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.vote.VoteAddRequest;
import com.cong.fishisland.model.dto.vote.VoteRecordRequest;
import com.cong.fishisland.model.vo.vote.VoteVO;
import com.cong.fishisland.service.VoteService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 投票接口
 */
@RestController
@RequestMapping("/vote")
@Slf4j
public class VoteController {

    @Resource
    private VoteService voteService;

    /**
     * 创建投票
     *
     * @param voteAddRequest 创建投票请求
     * @return 投票ID
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建投票")
    public BaseResponse<String> createVote(@RequestBody VoteAddRequest voteAddRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        String voteId = voteService.createVote(voteAddRequest, userId);
        return ResultUtils.success(voteId);
    }

    /**
     * 参与投票
     *
     * @param voteRecordRequest 投票请求
     * @return 是否成功
     */
    @PostMapping("/record")
    @ApiOperation(value = "参与投票")
    public BaseResponse<Boolean> vote(@RequestBody VoteRecordRequest voteRecordRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        voteService.vote(voteRecordRequest, userId);
        return ResultUtils.success(true);
    }

    /**
     * 获取投票结果
     *
     * @param voteId 投票ID
     * @return 投票结果
     */
    @GetMapping("/result/{voteId}")
    @ApiOperation(value = "获取投票结果")
    public BaseResponse<VoteVO> getVoteResult(@PathVariable String voteId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        VoteVO voteVO = voteService.getVoteResult(voteId, userId);
        return ResultUtils.success(voteVO);
    }

    /**
     * 获取活跃投票列表
     *
     * @return 投票ID列表
     */
    @GetMapping("/active/list")
    @ApiOperation(value = "获取活跃投票列表")
    public BaseResponse<List<String>> getActiveVoteIds() {
        List<String> voteIds = voteService.getActiveVoteIds();
        return ResultUtils.success(voteIds);
    }

    /**
     * 删除投票
     *
     * @param voteId 投票ID
     * @return 是否成功
     */
    @PostMapping("/delete/{voteId}")
    @ApiOperation(value = "删除投票")
    public BaseResponse<Boolean> deleteVote(@PathVariable String voteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        voteService.deleteVote(voteId, userId);
        return ResultUtils.success(true);
    }
}
