package com.cong.fishisland.controller.pet;

import cn.dev33.satoken.stp.StpUtil;
import com.cong.fishisland.common.BaseResponse;
import com.cong.fishisland.common.ResultUtils;
import com.cong.fishisland.model.vo.pet.PetTreasureVO;
import com.cong.fishisland.service.pet.PetTreasureService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pet/treasure")
public class PetTreasureController {
    private final PetTreasureService service;

    public PetTreasureController(PetTreasureService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public BaseResponse<PetTreasureVO> status() {
        return ResultUtils.success(service.status(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/start")
    public BaseResponse<PetTreasureVO> start(@RequestBody Start request) {
        return ResultUtils.success(service.start(StpUtil.getLoginIdAsLong(), request.getSiteId()));
    }

    @PostMapping("/finish")
    public BaseResponse<PetTreasureVO> finish(@RequestBody Finish request) {
        return ResultUtils.success(service.finish(StpUtil.getLoginIdAsLong(), request.getId()));
    }

    @Data
    public static class Start { private String siteId; }
    @Data
    public static class Finish { private String id; }
}
