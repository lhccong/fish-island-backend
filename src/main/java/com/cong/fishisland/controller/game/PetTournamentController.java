package com.cong.fishisland.controller.game;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.game.TournamentChallengeResultVO;
import com.cong.fishisland.model.vo.game.TournamentRankVO;
import com.cong.fishisland.service.PetTournamentService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宠物武道大会接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/pet/tournament")
@RequiredArgsConstructor
public class PetTournamentController {

    private final PetTournamentService petTournamentService;

    /**
     * 挑战指定位数
     * 规则：当前用户无排名，或排名比目标位数低（数字更大），才可挑战
     */
    @PostMapping("/challenge")
    @ApiOperation("挑战指定位数（无排名或排名更低才可挑战）")
    public BaseResponse<TournamentChallengeResultVO> challenge(@RequestParam int targetRank) {
        return ResultUtils.success(petTournamentService.challenge(targetRank));
    }

    /**
     * 获取当日排行榜
     */
    @GetMapping("/leaderboard")
    @ApiOperation("获取当日武道大会排行榜")
    public BaseResponse<List<TournamentRankVO>> getLeaderboard() {
        return ResultUtils.success(petTournamentService.getLeaderboard());
    }

    /**
     * 获取我的当前排名
     */
    @GetMapping("/my/rank")
    @ApiOperation("获取我的当前排名（无排名返回null）")
    public BaseResponse<Integer> getMyRank() {
        return ResultUtils.success(petTournamentService.getMyRank());
    }
}
