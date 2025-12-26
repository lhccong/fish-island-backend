package com.cong.fishisland.controller.game;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.game.BattleResultVO;
import com.cong.fishisland.model.vo.game.BossBattleInfoVO;
import com.cong.fishisland.model.vo.game.BossChallengeRankingVO;
import com.cong.fishisland.model.vo.game.BossVO;
import com.cong.fishisland.service.BossService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Boss接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/boss")
@Slf4j
//@Api(tags = "Boss接口")
public class BossController {

    @Resource
    private BossService bossService;

    /**
     * 获取Boss列表
     *
     * @return Boss列表
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取Boss列表")
    public BaseResponse<List<BossVO>> getBossList() {
        List<BossVO> bossList = bossService.getBossList();
        return ResultUtils.success(bossList);
    }

    /**
     * 对战方法（10个回合）
     *
     * @param bossId Boss ID
     * @return 10个回合的对战结果列表
     */
    @GetMapping("/battle")
    @ApiOperation(value = "对战Boss（10个回合，每天只能挑战一次）")
    public BaseResponse<List<BattleResultVO>> battle(@RequestParam Long bossId) {
        List<BattleResultVO> results = bossService.battle(bossId);
        return ResultUtils.success(results);
    }

    /**
     * 获取Boss挑战排行榜
     *
     * @param bossId Boss ID
     * @param limit 返回数量限制，默认10
     * @return 排行榜列表
     */
    @GetMapping("/ranking")
    @ApiOperation(value = "获取Boss挑战排行榜")
    public BaseResponse<List<BossChallengeRankingVO>> getBossChallengeRanking(
            @RequestParam Long bossId,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        List<BossChallengeRankingVO> ranking = bossService.getBossChallengeRanking(bossId, limit);
        return ResultUtils.success(ranking);
    }

    /**
     * 获取当前缓存中的Boss列表数据（包含实时血量）
     *
     * @return Boss列表（包含从Redis获取的当前血量）
     */
    @GetMapping("/list/cache")
    @ApiOperation(value = "获取当前缓存中的Boss列表数据（包含实时血量）")
    public BaseResponse<List<BossVO>> getBossListWithCache() {
        List<BossVO> bossList = bossService.getBossListWithCache();
        return ResultUtils.success(bossList);
    }

    /**
     * 获取Boss对战信息（包含当前用户的宠物信息和Boss信息）
     *
     * @param bossId Boss ID
     * @return Boss对战信息（包含宠物和Boss的详细信息）
     */
    @GetMapping("/battle/info")
    @ApiOperation(value = "获取Boss对战信息（包含当前用户的宠物信息和Boss信息）")
    public BaseResponse<BossBattleInfoVO> getBossBattleInfo(@RequestParam Long bossId) {
        BossBattleInfoVO battleInfo = bossService.getBossBattleInfo(bossId);
        return ResultUtils.success(battleInfo);
    }
}

