package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.CreatePetRequest;
import com.cong.fishisland.model.dto.pet.UpdatePetNameRequest;
import com.cong.fishisland.model.vo.pet.OtherUserPetVO;
import com.cong.fishisland.model.vo.pet.PetVO;
import com.cong.fishisland.service.FishPetService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 宠物接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/pet")
@Slf4j
//@Api(tags = "宠物接口")
public class FishPetController {

    @Resource
    private FishPetService fishPetService;

    /**
     * 创建宠物
     *
     * @param createPetRequest 创建宠物请求
     * @return 宠物ID
     */
    @PostMapping("/create")
    @ApiOperation(value = "创建宠物")
    public BaseResponse<Long> createPet(@RequestBody CreatePetRequest createPetRequest) {
        Long petId = fishPetService.createPet(createPetRequest);
        return ResultUtils.success(petId);
    }

    /**
     * 获取宠物详情
     *
     * @return 宠物详情
     */
    @GetMapping("/my/get")
    @ApiOperation(value = "获取宠物详情")
    public BaseResponse<PetVO> getPetDetail() {

        PetVO petVO = fishPetService.getPetDetail();
        return ResultUtils.success(petVO);
    }

    /**
     * 修改宠物名称
     *
     * @param updatePetNameRequest 修改宠物名称请求
     * @return 是否成功
     */
    @PostMapping("/update/name")
    @ApiOperation(value = "修改宠物名称")
    public BaseResponse<Boolean> updatePetName(@RequestBody UpdatePetNameRequest updatePetNameRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = fishPetService.updatePetName(updatePetNameRequest, userId);
        return ResultUtils.success(result);
    }

    /**
     * 查看其他用户的宠物
     *
     * @param otherUserId 其他用户ID
     * @return 宠物详情（不包含扩展数据）
     */
    @GetMapping("/other")
    @ApiOperation(value = "查看其他用户的宠物")
    public BaseResponse<OtherUserPetVO> getOtherUserPet(@RequestParam Long otherUserId) {
        OtherUserPetVO otherUserPetVO = fishPetService.getOtherUserPet(otherUserId);
        return ResultUtils.success(otherUserPetVO);
    }

    /**
     * 喂食宠物
     *
     * @param petId 宠物ID
     * @return 更新后的宠物详情
     */
    @PostMapping("/feed")
    @ApiOperation(value = "喂食宠物", notes = "消耗5积分，增加宠物饥饿度和心情值，有1小时冷却时间")
    public BaseResponse<PetVO> feedPet(@RequestParam Long petId) {
        PetVO petVO = fishPetService.feedPet(petId);
        return ResultUtils.success(petVO);
    }

    /**
     * 抚摸宠物
     *
     * @param petId 宠物ID
     * @return 更新后的宠物详情
     */
    @PostMapping("/pat")
    @ApiOperation(value = "抚摸宠物", notes = "消耗3积分，增加宠物心情值，有1小时冷却时间")
    public BaseResponse<PetVO> patPet(@RequestParam Long petId) {
        PetVO petVO = fishPetService.patPet(petId);
        return ResultUtils.success(petVO);
    }
} 