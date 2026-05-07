package com.cong.fishisland.controller.pet;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.ForgeDetailRequest;
import com.cong.fishisland.model.dto.pet.ForgeLockRequest;
import com.cong.fishisland.model.dto.pet.ForgeRefreshRequest;
import com.cong.fishisland.model.dto.pet.ForgeUpgradeRequest;
import com.cong.fishisland.model.vo.pet.PetEquipForgeDetailVO;
import com.cong.fishisland.model.vo.pet.PetEquipForgeVO;
import com.cong.fishisland.service.PetEquipForgeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
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
//@Api(tags = "宠物装备锻造接口")
public class PetEquipForgeController {

    @Resource
    private PetEquipForgeService petEquipForgeService;

    /**
     * 获取宠物所有装备锻造信息
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取宠物装备列表", notes = "根据宠物ID查询该宠物所有装备槽的锻造信息")
    @ApiImplicitParam(name = "petId", value = "宠物ID", required = true, dataType = "Long", paramType = "query", example = "1001")
    public BaseResponse<List<PetEquipForgeVO>> listByPetId(@RequestParam Long petId) {
        return ResultUtils.success(petEquipForgeService.listByPetId(petId));
    }

    /**
     * 查询单件装备锻造详情（含本次升级消耗积分和成功概率）
     */
    @PostMapping("/detail")
    @ApiOperation(value = "查询单件装备锻造详情",
            notes = "返回指定装备槽的词条属性、当前等级，以及本次升级所需积分和成功概率")
    public BaseResponse<PetEquipForgeDetailVO> getForgeDetail(@RequestBody ForgeDetailRequest request) {
        return ResultUtils.success(petEquipForgeService.getForgeDetail(request.getPetId(), request.getEquipSlot()));
    }

    /**
     * 锁定/解锁词条
     * 指定需要锁定的词条序号，未在列表中的词条将被解锁，传空列表表示解锁全部
     */
    @PostMapping("/lock")
    @ApiOperation(value = "锁定/解锁词条", notes = "指定需要锁定的词条序号（1~4），未在列表中的词条将被解锁，传空列表表示解锁全部")
    public BaseResponse<PetEquipForgeVO> lockEntries(@RequestBody ForgeLockRequest request) {
        return ResultUtils.success(petEquipForgeService.lockEntries(request));
    }

    /**
     * 刷新装备词条
     * 基础消耗 100 积分，每锁定一条额外 +50 积分
     */
    @PostMapping("/refresh")
    @ApiOperation(value = "刷新装备词条", notes = "基础消耗 100 积分，每有一条词条处于锁定状态额外 +50 积分，锁定的词条不会被刷新")
    public BaseResponse<PetEquipForgeVO> refreshEntries(@RequestBody ForgeRefreshRequest request) {
        return ResultUtils.success(petEquipForgeService.refreshEntries(request));
    }

    /**
     * 装备升级
     * 消耗积分随等级递增，成功概率随等级递减
     */
    @PostMapping("/upgrade")
    @ApiOperation(value = "装备升级", notes = "消耗积分随等级递增，成功概率随等级递减，返回是否升级成功")
    public BaseResponse<Boolean> upgradeEquip(@RequestBody ForgeUpgradeRequest request) {
        return ResultUtils.success(petEquipForgeService.upgradeEquip(request));
    }
}
