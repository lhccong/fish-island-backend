package com.cong.fishisland.controller.fishbattle;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.entity.fishbattle.FishBattleGame;
import com.cong.fishisland.model.entity.fishbattle.FishBattlePlayerStats;
import com.cong.fishisland.service.UserService;
import com.cong.fishisland.service.fishbattle.FishBattleGameService;
import com.cong.fishisland.service.fishbattle.FishBattlePlayerStatsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 摸鱼大乱斗对局接口
 */
@Api(tags = "摸鱼大乱斗-对局")
@RestController
@RequestMapping("/fishBattle/game")
@RequiredArgsConstructor
public class FishBattleGameController {

    private final FishBattleGameService fishBattleGameService;
    private final FishBattlePlayerStatsService fishBattlePlayerStatsService;
    private final UserService userService;

    @ApiOperation("获取对局详情（含所有玩家统计）")
    @GetMapping("/{gameId}")
    public BaseResponse<Map<String, Object>> getGameDetail(@PathVariable Long gameId) {
        userService.getLoginUser();
        FishBattleGame game = fishBattleGameService.getGameDetail(gameId);
        if (game == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "对局不存在");
        }
        List<FishBattlePlayerStats> playerStats = fishBattlePlayerStatsService.listByGameId(gameId);

        Map<String, Object> result = new HashMap<>();
        result.put("game", game);
        result.put("playerStats", playerStats);
        return ResultUtils.success(result);
    }

    @ApiOperation("点赞玩家")
    @PostMapping("/like")
    public BaseResponse<Boolean> likePlayer(@RequestBody Map<String, Long> params) {
        userService.getLoginUser();
        Long gameId = params.get("gameId");
        Long targetUserId = params.get("targetUserId");
        if (gameId == null || targetUserId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        return ResultUtils.success(fishBattlePlayerStatsService.likePlayer(gameId, targetUserId));
    }
}
