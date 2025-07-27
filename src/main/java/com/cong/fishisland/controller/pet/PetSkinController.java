package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cong.fishisland.annotation.NoRepeatSubmit;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.dto.pet.PetSkinExchangeRequest;
import com.cong.fishisland.model.dto.pet.PetSkinQueryRequest;
import com.cong.fishisland.model.dto.pet.PetSkinSetRequest;
import com.cong.fishisland.model.vo.pet.PetSkinVO;
import com.cong.fishisland.model.vo.pet.PetVO;
import com.cong.fishisland.service.PetSkinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * 宠物皮肤控制器
 *
 * @author cong
 */
@RestController
@RequestMapping("/api/pet/skin")
@RequiredArgsConstructor
public class PetSkinController {

    private final PetSkinService petSkinService;

    /**
     * 分页查询宠物皮肤
     *
     * @param petSkinQueryRequest 查询请求
     * @return 宠物皮肤分页结果
     */
    @GetMapping("/list")
    @SaCheckLogin
    public BaseResponse<Page<PetSkinVO>> listPetSkins(PetSkinQueryRequest petSkinQueryRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<PetSkinVO> petSkinPage = petSkinService.queryPetSkinByPage(petSkinQueryRequest, userId);
        return ResultUtils.success(petSkinPage);
    }

    /**
     * 兑换宠物皮肤
     *
     * @param petSkinExchangeRequest 兑换请求
     * @return 是否兑换成功
     */
    @PostMapping("/exchange")
    @SaCheckLogin
    @NoRepeatSubmit
    public BaseResponse<Boolean> exchangePetSkin(@RequestBody PetSkinExchangeRequest petSkinExchangeRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = petSkinService.exchangePetSkin(petSkinExchangeRequest, userId);
        return ResultUtils.success(result);
    }

    /**
     * 设置宠物皮肤
     *
     * @param petSkinSetRequest 设置请求
     * @return 更新后的宠物信息
     */
    @PostMapping("/set")
    @SaCheckLogin
    public BaseResponse<PetVO> setPetSkin(@RequestBody PetSkinSetRequest petSkinSetRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        PetVO petVO = petSkinService.setPetSkin(petSkinSetRequest, userId);
        return ResultUtils.success(petVO);
    }
} 