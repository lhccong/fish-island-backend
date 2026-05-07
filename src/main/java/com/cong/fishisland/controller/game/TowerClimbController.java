package com.cong.fishisland.controller.game;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.game.TowerClimbResultVO;
import com.cong.fishisland.model.vo.game.TowerFloorMonsterVO;
import com.cong.fishisland.model.vo.game.TowerProgressVO;
import com.cong.fishisland.model.vo.game.TowerRankVO;
import com.cong.fishisland.service.TowerClimbService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 爬塔接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/tower")
@RequiredArgsConstructor
public class TowerClimbController {

    private final TowerClimbService towerClimbService;

    /**
     * 获取当前用户爬塔进度及下一层怪物信息
     */
    @GetMapping("/progress")
    @ApiOperation("获取爬塔进度")
    public BaseResponse<TowerProgressVO> getProgress() {
        return ResultUtils.success(towerClimbService.getProgress());
    }

    /**
     * 查看指定层的怪物信息
     */
    @GetMapping("/floor")
    @ApiOperation("查看指定层怪物信息")
    public BaseResponse<TowerFloorMonsterVO> getFloorMonster(@RequestParam int floor) {
        return ResultUtils.success(towerClimbService.getFloorMonster(floor));
    }

    /**
     * 挑战下一层
     */
    @PostMapping("/challenge")
    @ApiOperation("挑战下一层")
    public BaseResponse<TowerClimbResultVO> challenge() {
        return ResultUtils.success(towerClimbService.challenge());
    }

    /**
     * 爬塔排行榜
     */
    @GetMapping("/ranking")
    @ApiOperation("爬塔排行榜（按最高通关层数降序）")
    public BaseResponse<List<TowerRankVO>> getRanking(
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return ResultUtils.success(towerClimbService.getRanking(limit));
    }
}
