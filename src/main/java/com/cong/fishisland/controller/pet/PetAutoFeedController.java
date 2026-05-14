package com.cong.fishisland.controller.pet;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.PetAutoFeedConfigRequest;
import com.cong.fishisland.model.vo.pet.PetAutoFeedConfigVO;
import com.cong.fishisland.service.PetAutoFeedService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 宠物自动喂食接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/pet/autoFeed")
@Slf4j
public class PetAutoFeedController {

    @Resource
    private PetAutoFeedService petAutoFeedService;

    /**
     * 保存或更新自动喂食配置
     * 同一宠物只有一条配置，重复调用会覆盖
     */
    @PostMapping("/config")
    @ApiOperation(value = "保存/更新自动喂食配置", notes = "设置宠物自动喂食的食物类型和触发阈值，同一宠物只有一条配置")
    public BaseResponse<PetAutoFeedConfigVO> saveOrUpdateConfig(@RequestBody PetAutoFeedConfigRequest request) {
        return ResultUtils.success(petAutoFeedService.saveOrUpdateConfig(request));
    }

    /**
     * 获取当前用户指定宠物的自动喂食配置
     */
    @GetMapping("/config")
    @ApiOperation(value = "获取自动喂食配置", notes = "获取当前用户指定宠物的自动喂食配置，未配置则返回null")
    public BaseResponse<PetAutoFeedConfigVO> getConfig(@RequestParam Long petId) {
        return ResultUtils.success(petAutoFeedService.getConfig(petId));
    }

    /**
     * 开启或关闭自动喂食
     * 必须先通过 /config 接口保存配置，才能调用此接口切换开关
     */
    @PostMapping("/toggle")
    @ApiOperation(value = "开启/关闭自动喂食", notes = "enabled=1 开启，enabled=0 关闭；需先保存配置")
    public BaseResponse<PetAutoFeedConfigVO> toggleAutoFeed(@RequestParam Long petId, @RequestParam int enabled) {
        return ResultUtils.success(petAutoFeedService.toggleAutoFeed(petId, enabled));
    }

    // /**
    //  * 购买食物
    //  * 消耗积分，食物直接进入背包（可叠加）
    //  */
    // @PostMapping("/buyFood")
    // @ApiOperation(value = "购买食物", notes = "消耗积分购买指定食物，食物进入背包后可用于自动喂食")
    // public BaseResponse<Boolean> buyFood(@RequestParam String foodCode, @RequestParam int quantity) {
    //     petAutoFeedService.buyFood(foodCode, quantity);
    //     return ResultUtils.success(true);
    // }
}
