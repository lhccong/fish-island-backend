package com.cong.fishisland.controller.pet;

import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ErrorCode;
import com.cong.fishisland.common.exception.BusinessException;
import com.cong.fishisland.model.vo.pet.PetRankVO;
import com.cong.fishisland.service.FishPetService;
import com.cong.fishisland.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 宠物排行榜接口
 *
 * @author cong
 */
@RestController
@RequestMapping("/api/pet/rank")
@RequiredArgsConstructor
@Slf4j
public class PetRankController {

    private final FishPetService fishPetService;

    /**
     * 获取宠物排行榜
     *
     * @param limit 获取数量，默认为10
     * @return 宠物排行榜列表
     */
    @GetMapping("/list")
    public BaseResponse<List<PetRankVO>> getPetRankList(@RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "获取数量必须在1-50之间");
        }
        
        List<PetRankVO> petRankList = fishPetService.getPetRankList(limit);
        return ResultUtils.success(petRankList);
    }
} 