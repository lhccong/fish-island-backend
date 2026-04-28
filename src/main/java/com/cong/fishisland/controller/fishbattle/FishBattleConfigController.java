package com.cong.fishisland.controller.fishbattle;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.service.fishbattle.FishBattleConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 摸鱼大乱斗游戏配置接口
 */
@Slf4j
@Api(tags = "摸鱼大乱斗-游戏配置")
@RestController
@RequestMapping("/fishBattle/config")
@RequiredArgsConstructor
public class FishBattleConfigController {

    private final FishBattleConfigService fishBattleConfigService;
    private final ObjectMapper objectMapper;

    @ApiOperation("获取地图场景配置")
    @GetMapping("/map")
    public BaseResponse<JsonNode> getMapConfig() {
        return getConfigByKey("map_default");
    }

    @ApiOperation("获取游戏主配置")
    @GetMapping("/game")
    public BaseResponse<JsonNode> getGameConfig() {
        return getConfigByKey("game_default");
    }

    @ApiOperation("根据configKey获取配置")
    @GetMapping("/{configKey}")
    public BaseResponse<JsonNode> getConfig(@PathVariable String configKey) {
        return getConfigByKey(configKey);
    }

    private BaseResponse<JsonNode> getConfigByKey(String configKey) {
        String configData = fishBattleConfigService.getConfigData(configKey);
        if (configData == null) {
            return ResultUtils.error(40400, "配置不存在: " + configKey);
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(configData);
            return ResultUtils.success(jsonNode);
        } catch (Exception e) {
            log.error("解析配置JSON失败: configKey={}", configKey, e);
            return ResultUtils.error(50000, "配置数据格式异常");
        }
    }
}
