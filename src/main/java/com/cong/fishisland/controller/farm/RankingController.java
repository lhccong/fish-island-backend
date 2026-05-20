package com.cong.fishisland.controller.farm;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.farm.RankingDTO;
import com.cong.fishisland.service.FarmRankingService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ranking")
public class RankingController {

    @Autowired
    private FarmRankingService rankingService;

    @GetMapping("/steal/exp/today")
    @ApiOperation(value = "获取今日偷取经验排行榜")
    public BaseResponse<List<RankingDTO>> getTodayStealExpRanking() {
        List<RankingDTO> rankings = rankingService.getTodayStealExpRanking();
        return ResultUtils.success(rankings);
    }

    @GetMapping("/steal/exp/total")
    @ApiOperation(value = "获取累计偷取经验排行榜")
    public BaseResponse<List<RankingDTO>> getTotalStealExpRanking() {
        List<RankingDTO> rankings = rankingService.getTotalStealExpRanking();
        return ResultUtils.success(rankings);
    }

    @GetMapping("/steal/count/today")
    @ApiOperation(value = "获取今日偷取次数排行榜")
    public BaseResponse<List<RankingDTO>> getTodayStealCountRanking() {
        List<RankingDTO> rankings = rankingService.getTodayStealCountRanking();
        return ResultUtils.success(rankings);
    }

    @GetMapping("/steal/count/total")
    @ApiOperation(value = "获取累计偷取次数排行榜")
    public BaseResponse<List<RankingDTO>> getTotalStealCountRanking() {
        List<RankingDTO> rankings = rankingService.getTotalStealCountRanking();
        return ResultUtils.success(rankings);
    }

    @GetMapping("/defense/today")
    @ApiOperation(value = "获取今日防守排行榜")
    public BaseResponse<List<RankingDTO>> getTodayDefenseRanking() {
        List<RankingDTO> rankings = rankingService.getTodayDefenseRanking();
        return ResultUtils.success(rankings);
    }

    @GetMapping("/defense/total")
    @ApiOperation(value = "获取累计防守排行榜")
    public BaseResponse<List<RankingDTO>> getTotalDefenseRanking() {
        List<RankingDTO> rankings = rankingService.getTotalDefenseRanking();
        return ResultUtils.success(rankings);
    }
}
