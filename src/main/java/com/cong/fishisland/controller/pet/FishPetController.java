package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.model.dto.pet.CreatePetRequest;
import com.cong.fishisland.model.dto.pet.UpdatePetNameRequest;
import com.cong.fishisland.model.vo.pet.OtherUserPetVO;
import com.cong.fishisland.model.vo.pet.PetVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 宠物接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
@Slf4j
public class FishPetController {

    private final FishPetService fishPetService;

    /**
     * 创建宠物
     *
     * @param createPetRequest 创建宠物请求
     * @return 宠物ID
     */
    @PostMapping("/create")
    @SaCheckLogin
    public BaseResponse<Long> createPet(@RequestBody CreatePetRequest createPetRequest) {
        Long petId = fishPetService.createPet(createPetRequest);
        return ResultUtils.success(petId);
    }

    /**
     * 获取宠物详情
     *
     * @return 宠物详情
     */
    @GetMapping("/detail")
    @SaCheckLogin
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
    @SaCheckLogin
    public BaseResponse<Boolean> updatePetName(@RequestBody UpdatePetNameRequest updatePetNameRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = fishPetService.updatePetName(updatePetNameRequest, userId);
        return ResultUtils.success(result);
    }

    /**
     * 查看其他用户的宠物
     *
     * @param userId 用户ID
     * @return 宠物详情
     */
    @GetMapping("/other/{userId}")
    public BaseResponse<OtherUserPetVO> getOtherUserPet(@PathVariable Long userId) {
        OtherUserPetVO otherUserPetVO = fishPetService.getOtherUserPet(userId);
        return ResultUtils.success(otherUserPetVO);
    }

    /**
     * 喂食宠物
     *
     * @param petId 宠物ID
     * @return 更新后的宠物详情
     */
    @PostMapping("/feed/{petId}")
    @SaCheckLogin
    public BaseResponse<PetVO> feedPet(@PathVariable Long petId) {
        PetVO petVO = fishPetService.feedPet(petId);
        return ResultUtils.success(petVO);
    }

    /**
     * 抚摸宠物
     *
     * @param petId 宠物ID
     * @return 更新后的宠物详情
     */
    @PostMapping("/pat/{petId}")
    @SaCheckLogin
    public BaseResponse<PetVO> patPet(@PathVariable Long petId) {
        PetVO petVO = fishPetService.patPet(petId);
        return ResultUtils.success(petVO);
    }
} 