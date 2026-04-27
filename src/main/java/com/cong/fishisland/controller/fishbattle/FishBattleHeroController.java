package com.cong.fishisland.controller.fishbattle;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.entity.fishbattle.FishBattleHero;
import com.cong.fishisland.service.fishbattle.FishBattleHeroService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 摸鱼大乱斗英雄接口
 */
@Api(tags = "摸鱼大乱斗-英雄")
@RestController
@RequestMapping("/fishBattle/hero")
@RequiredArgsConstructor
public class FishBattleHeroController {

    private final FishBattleHeroService fishBattleHeroService;

    @ApiOperation("获取英雄列表")
    @GetMapping("/list")
    public BaseResponse<List<FishBattleHero>> listHeroes() {
        return ResultUtils.success(fishBattleHeroService.listEnabledHeroes());
    }
}
