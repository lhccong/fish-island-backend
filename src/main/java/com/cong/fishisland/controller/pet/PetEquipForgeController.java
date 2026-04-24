package com.cong.fishisland.controller.pet;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.vo.pet.PetEquipForgeVO;
import com.cong.fishisland.service.PetEquipForgeService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 宠物装备锻造接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/pet/forge")
@Slf4j
public class PetEquipForgeController {

    @Resource
    private PetEquipForgeService petEquipForgeService;

    /**
     * 获取宠物所有装备锻造信息
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取宠物装备列表")
    public BaseResponse<List<PetEquipForgeVO>> listByPetId(@RequestParam Long petId) {
        return ResultUtils.success(petEquipForgeService.listByPetId(petId));
    }

    /**
     * 刷新装备词条
     * 基础消耗 100 积分，每锁定一条额外 +50 积分
     */
    @PostMapping("/refresh")
    @ApiOperation(value = "刷新装备词条")
    public BaseResponse<PetEquipForgeVO> refreshEntries(@RequestBody ForgeRefreshRequest request) {
        return ResultUtils.success(petEquipForgeService.refreshEntries(request));
    }

    /**
     * 装备升级（武器不支持）
     * 消耗积分随等级递增，成功概率随等级递减
     */
    @PostMapping("/upgrade")
    @ApiOperation(value = "装备升级")
    public BaseResponse<Boolean> upgradeEquip(@RequestBody ForgeUpgradeRequest request) {
        return ResultUtils.success(petEquipForgeService.upgradeEquip(request));
    }
}
